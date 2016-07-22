/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: Eurotech
 *******************************************************************************/
package org.eclipse.kura.core.deployment.download.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.CancellationException;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.download.DeploymentDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.DownloadOps;
import org.eclipse.kura.core.deployment.install.DeploymentInstallOptions;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.core.deployment.progress.impl.ProgressEventImpl;
import org.eclipse.kura.core.deployment.util.FileUtilities;
import org.eclipse.kura.core.deployment.util.HashUtil;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.ssl.SslManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadOpsImpl implements ProgressListener, DownloadOps {
    private static final Logger s_logger          = LoggerFactory.getLogger(DownloadOpsImpl.class);
    public static final String  RESOURCE_DOWNLOAD = "download";

    private CloudDeploymentHandlerV2         callback;
    private DeploymentDownloadOptions options;               // TODO:
                                                                    // rename
                                                                    // this
                                                                    // class
    private DownloadCountingOutputStream     downloadHelper;
    private SslManagerService                sslManagerService;
    private boolean                          alreadyDownloadedFlag;
    private String                           verificationDirectory;

    public DownloadOpsImpl() {
    }

    // ----------------------------------------------------------------
    //
    // Public methods
    //
    // ----------------------------------------------------------------

    public DeploymentDownloadOptions getDownloadOptions() {
        return options;
    }
    
    public boolean isDownloadInProgress() {
        return (options != null || callback != null);
    }

    public void setDownloadOptions(DeploymentDownloadOptions options) {
        this.options = options;
    }

    public void setCallback(CloudDeploymentHandlerV2 callback) {
        this.callback = callback;
    }

    public DownloadCountingOutputStream getDownloadHelper() {
        return downloadHelper;
    }

    public void setSslManager(SslManagerService sslManager) {
        this.sslManagerService = sslManager;
    }

    public void setAlreadyDownloadedFlag(boolean alreadyDownloaded) {
        this.alreadyDownloadedFlag = alreadyDownloaded;
    }

    public void setVerificationDirectory(String verificationDirectory) {
        this.verificationDirectory = verificationDirectory;
    }

    @Override
    public void progressChanged(ProgressEventImpl progress) {

        s_logger.info("{}% downloaded", progress.getTransferProgress());

        DownloadNotifyPayloadImpl notify = new DownloadNotifyPayloadImpl(progress.getClientId());
        notify.setTimestamp(new Date());
        notify.setTransferSize(progress.getTransferSize());
        notify.setTransferProgress(progress.getTransferProgress());
        notify.setTransferStatus(progress.getTransferStatus());
        notify.setJobId(progress.getJobId());
        if (progress.getExceptionMessage() != null) {
            notify.setErrorMessage(progress.getExceptionMessage());
        }

        notify.setTransferIndex(progress.getDownloadIndex());

        callback.publishNotificationMessage(options, notify, RESOURCE_DOWNLOAD);
    }

    public File downloadDeploymentPackageInternal() throws KuraException {
        File dpFile = null;
        int downloadIndex = 0;
        try {
            // Download the package to a temporary file.
            // Check for file existence has already been done
            dpFile = DownloadFileUtilitiesImpl.getDownloadFile(options);
            boolean forceDownload = options.isDownloadForced();

            if (!alreadyDownloadedFlag || forceDownload) {
                s_logger.info("To download");
                incrementalDownloadFromURL(dpFile, options.getDeployUri(), downloadIndex);
                downloadIndex++;

                if (options.getVerifierURL() != null) {
                    File dpVerifier = getDpVerifierFile(options);
                    incrementalDownloadFromURL(dpVerifier, options.getVerifierURL(), downloadIndex);
                }
            } else {
                alreadyDownloadedAsync();
            }
        } catch (CancellationException ce) {
            s_logger.error("Download exception", ce);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ce);
        } catch (Exception e) {
            s_logger.info("Download exception", e);
            downloadFailedAsync(downloadIndex);
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        }
        return dpFile;
    }
    

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------

    private void incrementalDownloadFromURL(File dpFile, String url, int downloadIndex) throws Exception {
        OutputStream os = null;

        try {
            os = new FileOutputStream(dpFile);
            DownloadOptionsImpl downloadOptions = new DownloadOptionsImpl();
            downloadOptions.setOutputStream(os);
            downloadOptions.setRequestOptions(options);
            downloadOptions.setCallback(this);
            downloadOptions.setSslManagerService(sslManagerService);
            downloadOptions.setDownloadURL(url);
            downloadOptions.setAlreadyDownloaded(downloadIndex);

            downloadHelper = DownloadFactoryImpl.getDownloadInstance(options.getDownloadProtocol(), downloadOptions);
            downloadHelper.startWork();
            downloadHelper.close();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e1) {
                    s_logger.error("Exception while trying to close stream.", e1);
                }
            }
        }

        if (options.getHash() != null) {
            String[] hashAlgorithmValue = options.getHash().split(":");

            String hashAlgorithm = null;
            String hashValue = null;
            if (hashAlgorithmValue.length == 2) {
                hashAlgorithm = hashAlgorithmValue[0].trim();
                hashValue = hashAlgorithmValue[1].trim();
            }
            s_logger.info("--> Going to verify hash signature!");
            try {
                String checksum = HashUtil.hash(hashAlgorithm, dpFile);
                if (hashAlgorithm == null
                        || "".equals(hashAlgorithm)
                        || hashValue == null
                        || "".equals(hashValue)
                        || checksum == null
                        || !checksum.equals(hashValue)) {
                    throw new KuraException(KuraErrorCode.INTERNAL_ERROR, null, "Failed to verify checksum with algorithm: " + hashAlgorithm);
                }
            } catch (Exception e) {
                dpFile.delete();
                throw e;
            }
        }
    }

    // Synchronous messages
    public static void downloadInProgressSyncMessage(KuraResponsePayload respPayload, DownloadCountingOutputStream downloadHelper, DeploymentDownloadOptions downloadOptions) {
        respPayload.setTimestamp(new Date());
        respPayload.addMetric(DownloadNotifyPayloadImpl.METRIC_TRANSFER_SIZE, downloadHelper.getTotalBytes().intValue());
        respPayload.addMetric(DownloadNotifyPayloadImpl.METRIC_TRANSFER_PROGRESS, downloadHelper.getDownloadTransferProgressPercentage().intValue());
        respPayload.addMetric(DownloadNotifyPayloadImpl.METRIC_TRANSFER_STATUS, downloadHelper.getDownloadTransferStatus().getStatusString());
        respPayload.addMetric(DownloadNotifyPayloadImpl.METRIC_JOB_ID, downloadOptions.getJobId());
    }

    public static void downloadAlreadyDoneSyncMessage(KuraResponsePayload respPayload) {
        respPayload.setTimestamp(new Date());
        respPayload.addMetric(DownloadNotifyPayloadImpl.METRIC_TRANSFER_SIZE, 0);
        respPayload.addMetric(DownloadNotifyPayloadImpl.METRIC_TRANSFER_PROGRESS, 100);
        respPayload.addMetric(DownloadNotifyPayloadImpl.METRIC_TRANSFER_STATUS, DownloadStatusImpl.ALREADY_DONE);
    }

    private void alreadyDownloadedAsync() {
        DownloadNotifyPayloadImpl notify = new DownloadNotifyPayloadImpl(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setTransferSize(0);
        notify.setTransferProgress(100);
        notify.setTransferStatus(DownloadStatusImpl.COMPLETED.getStatusString());
        notify.setJobId(options.getJobId());

        callback.publishNotificationMessage(options, notify, RESOURCE_DOWNLOAD);
    }

    private void downloadFailedAsync(int downloadIndex) {
        DownloadNotifyPayloadImpl notify = new DownloadNotifyPayloadImpl(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setTransferSize(0);
        notify.setTransferProgress(0);
        notify.setTransferStatus(DownloadStatusImpl.FAILED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setErrorMessage("Error during download process and verification!"); // message
                                                                                   // to
                                                                                   // get
                                                                                   // cause
        notify.setTransferIndex(downloadIndex);

        callback.publishNotificationMessage(options, notify, RESOURCE_DOWNLOAD);
    }

    private File getDpVerifierFile(DeploymentInstallOptions options) throws IOException {

        String shName = FileUtilities.getFileName(options.getDpName(), options.getDpVersion(), "_verifier.sh");
        String packageFilename = new StringBuilder().append(verificationDirectory)
                .append(File.separator)
                .append(shName)
                .toString();

        return new File(packageFilename);
    }
}
