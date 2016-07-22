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
package org.eclipse.kura.core.deployment.install.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.DeploymentOptions;
import org.eclipse.kura.core.deployment.download.DeploymentDownloadOptions;
import org.eclipse.kura.core.deployment.download.impl.DeploymentDownloadOptionsImpl;
import org.eclipse.kura.core.deployment.DeploymentOptionsImpl;
import org.eclipse.kura.core.deployment.install.DeploymentInstallOptions;
import org.eclipse.kura.core.deployment.install.InstallOps;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstallOpsImpl implements InstallOps {
    private static final Logger s_logger = LoggerFactory.getLogger(InstallOpsImpl.class);
    
    private static final int    PROGRESS_COMPLETE                        = 100;
    private static final String MESSAGE_CONFIGURATION_FILE_NOT_SPECIFIED = "Configuration file not specified";

    private static final InstallOpsImpl instance = new InstallOpsImpl();

    private DeploymentInstallOptions options;
    private CloudDeploymentHandlerV2 callback;
    private DeploymentAdmin          deploymentAdmin;
    private Properties               installPersistance;
    private String                   dpaConfPath;
    private String                   installVerifDir;
    private String                   installPersistanceLocation;
    private String                   packagesPath;

    public static InstallOpsImpl getInstance() {
        return instance;
    }

    @Override
    public void cleanup() {
        options = null;
        callback = null;
    }

    @Override
    public void initPersistance(String kuraDataDir) {
        StringBuilder pathSB = new StringBuilder();
        pathSB.append(kuraDataDir);
        pathSB.append(File.separator);
        pathSB.append(PERSISTANCE_FOLDER_NAME);
        installPersistanceLocation = pathSB.toString();
        File installPersistanceDir = new File(installPersistanceLocation);
        if (!installPersistanceDir.exists()) {
            installPersistanceDir.mkdir();
        }

        pathSB = new StringBuilder();
        pathSB.append(installPersistanceLocation);
        pathSB.append(File.separator);
        pathSB.append(PERSISTANCE_VERIFICATION_FOLDER_NAME);
        installVerifDir = pathSB.toString();
        File installVerificationDir = new File(installVerifDir);
        if (!installVerificationDir.exists()) {
            installVerificationDir.mkdir();
        }
    }

    @Override
    public void setCallback(CloudDeploymentHandlerV2 callback) {
        this.callback = callback;
    }

    @Override
    public boolean isInstalling() {
        return options != null || callback != null;
    }

    @Override
    public Properties getDeployedPackages() {
        FileInputStream fis = null;
        Properties deployedPackages = new Properties();
        try {
            fis = new FileInputStream(dpaConfPath);
            deployedPackages.load(fis);
        } catch (IOException e) {
            s_logger.error("Error opening package configuration file", e);
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                s_logger.error("Exception while closing opened resources!", e);
            }
        }
        return deployedPackages;
    }

    @Override
    public void setOptions(DeploymentInstallOptions options) {
        this.options = options;
    }

    @Override
    public DeploymentInstallOptions getOptions() {
        return options;
    }

    @Override
    public void setPackagesPath(String packagesPath) {
        this.packagesPath = packagesPath;
    }

    @Override
    public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = deploymentAdmin;
    }

    @Override
    public void setDpaConfPath(String dpaConfPath) {
        this.dpaConfPath = dpaConfPath;
    }

    @Override
    public void installDp(DeploymentInstallOptions options, File dpFile) throws KuraException {
        SafeProcess proc = null;
        try {
            installDeploymentPackageInternal(dpFile);
            installCompleteAsync(options, dpFile.getName());
            s_logger.info("Install completed!");

            if (options.isReboot()) {
                Thread.sleep(options.getRebootDelay());
                proc = ProcessUtil.exec("reboot");
            }
        } catch (Exception e) {
            s_logger.info("Install failed!");
            installFailedAsync(options, dpFile.getName(), e);
        } finally {
            if (proc != null) {
                ProcessUtil.destroy(proc);
            }
        }
    }

    @Override
    public void installSh(DeploymentOptions options, File shFile) throws KuraException {
        updateInstallPersistance(shFile.getName(), options);

        shFile.setExecutable(true);

        SafeProcess proc2 = null;
        try {
            proc2 = ProcessUtil.exec(shFile.getCanonicalPath());
        } catch (IOException e) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
        } finally {
            if (proc2 != null) {
                ProcessUtil.destroy(proc2);
            }
        }
    }

    @Override
    public void installInProgressSyncMessage(KuraResponsePayload respPayload) {
        respPayload.setTimestamp(new Date());
        respPayload.addMetric(InstallNotifyPayloadImpl.METRIC_INSTALL_STATUS, InstallStatus.IN_PROGRESS);
        respPayload.addMetric(InstallNotifyPayloadImpl.METRIC_DP_NAME, options.getDpName());
        respPayload.addMetric(InstallNotifyPayloadImpl.METRIC_DP_VERSION, options.getDpVersion());
    }

    @Override
    public void installIdleSyncMessage(KuraResponsePayload respPayload) {
        respPayload.setTimestamp(new Date());
        respPayload.addMetric(InstallNotifyPayloadImpl.METRIC_INSTALL_STATUS, InstallStatus.IDLE);
    }

    @Override
    public void installCompleteAsync(DeploymentOptions options, String dpName) throws KuraException {
        InstallNotifyPayloadImpl notify = new InstallNotifyPayloadImpl(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setInstallStatus(InstallStatus.COMPLETED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setDpName(dpName); // Probably split dpName and dpVersion?
        notify.setInstallProgress(PROGRESS_COMPLETE);

        callback.publishNotificationMessage(options, notify, RESOURCE_INSTALL);
    }

    @Override
    public void installFailedAsync(DeploymentInstallOptions options, String dpName, Exception e) throws KuraException {
        InstallNotifyPayloadImpl notify = new InstallNotifyPayloadImpl(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setInstallStatus(InstallStatus.FAILED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setDpName(dpName); // Probably split dpName and dpVersion?
        notify.setInstallProgress(0);
        if (e != null) {
            notify.setErrorMessage(e.getMessage());
        }

        callback.publishNotificationMessage(options, notify, RESOURCE_INSTALL);
    }

    @Override
    public void sendInstallConfirmations() {
        s_logger.info("Ready to send Confirmations");
        File verificationDir = new File(installVerifDir);
        if (verificationDir.listFiles() != null) {
            for (File fileEntry : verificationDir.listFiles()) {
                if (fileEntry.isFile() && fileEntry.getName().endsWith(".sh")) {
                    fileEntry.setExecutable(true);

                    SafeProcess proc2 = null;
                    try {
                        proc2 = ProcessUtil.exec(fileEntry.getCanonicalPath());
                        int exitValue = proc2.exitValue();
                        if (exitValue == 0) {
                            sendSysUpdateSuccess();
                        } else {
                            sendSysUpdateFailure();
                        }
                    } catch (Exception e) {

                    } finally {
                        fileEntry.delete();
                        if (proc2 != null) {
                            ProcessUtil.destroy(proc2);
                        }
                    }

                }
            }
        }
    }
    
    @Override
    public void removePackageFromConfFile(String packageName) {
        Properties deployedPackages = getDeployedPackages();
        deployedPackages.remove(packageName);

        if (dpaConfPath == null) {
            s_logger.warn(MESSAGE_CONFIGURATION_FILE_NOT_SPECIFIED);
            return;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dpaConfPath);
            deployedPackages.store(fos, null);
            fos.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            s_logger.error("Error writing package configuration file", e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                s_logger.error("Exception while closing opened resources!", e);
            }
        }
    }

    private DeploymentPackage installDeploymentPackageInternal(File fileReference)
            throws DeploymentException, IOException {

        InputStream dpInputStream = null;
        DeploymentPackage dp = null;
        File dpPersistentFile = null;
        File downloadedFile = fileReference;

        try {
            String dpBasename = fileReference.getName();
            StringBuilder pathSB = new StringBuilder();
            pathSB.append(packagesPath);
            pathSB.append(File.separator);
            pathSB.append(dpBasename);
            String dpPersistentFilePath = pathSB.toString();
            
            dpPersistentFile = new File(dpPersistentFilePath);
            dpInputStream = new FileInputStream(downloadedFile);
            dp = deploymentAdmin.installDeploymentPackage(dpInputStream);

            // Now we need to copy the deployment package file to the Kura
            // packages directory unless it's already there.

            if (!downloadedFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
                s_logger.debug("dpFile.getCanonicalPath(): {}", downloadedFile.getCanonicalPath());
                s_logger.debug("dpPersistentFile.getCanonicalPath(): {}", dpPersistentFile.getCanonicalPath());
                FileUtils.copyFile(downloadedFile, dpPersistentFile);
                addPackageToConfFile(dp.getName(), "file:" + dpPersistentFilePath);
            }
        } catch (FileNotFoundException ex) {

        } catch (IOException ex) {

        } finally {
            if (dpInputStream != null) {
                try {
                    dpInputStream.close();
                } catch (IOException e) {
                    s_logger.warn("Cannot close input stream", e);
                }
            }
            // The file from which we have installed the deployment package will
            // be deleted
            // unless it's a persistent deployment package file.
            if (downloadedFile != null &&
                    dpPersistentFile != null &&
                    !downloadedFile.getCanonicalPath().equals(dpPersistentFile.getCanonicalPath())) {
                downloadedFile.delete();
            }
        }

        return dp;
    }

    private void updateInstallPersistance(String fileName, DeploymentOptions options) {
        installPersistance = new Properties();
        installPersistance.setProperty(DeploymentOptionsImpl.METRIC_DP_CLIENT_ID, options.getClientId());
        installPersistance.setProperty(DeploymentOptionsImpl.METRIC_JOB_ID, Long.toString(options.getJobId()));
        installPersistance.setProperty(DeploymentOptionsImpl.METRIC_DP_NAME, fileName);
        installPersistance.setProperty(DeploymentOptionsImpl.METRIC_DP_VERSION, options.getDpVersion());
        installPersistance.setProperty(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID, options.getRequestClientId());
        installPersistance.setProperty(PERSISTANCE_FILE_NAME, fileName);

        if (installPersistanceLocation == null) {
            s_logger.warn(MESSAGE_CONFIGURATION_FILE_NOT_SPECIFIED);
            return;
        }

        FileOutputStream fos = null;
        try {
            StringBuilder pathSB = new StringBuilder();
            pathSB.append(installPersistanceLocation);
            pathSB.append(File.separator);
            pathSB.append(fileName);
            pathSB.append(PERSISTANCE_SUFFIX);
            String persistanceFile = pathSB.toString();
            fos = new FileOutputStream(persistanceFile);
            installPersistance.store(fos, null);
            fos.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            s_logger.error("Error writing remote install configuration file", e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    s_logger.error("Exception while closing the stream!", e);
                }
            }
        }
    }

    private void addPackageToConfFile(String packageName, String packageUrl) {
        Properties deployedPackages = getDeployedPackages();
        deployedPackages.setProperty(packageName, packageUrl);

        if (dpaConfPath == null) {
            s_logger.warn(MESSAGE_CONFIGURATION_FILE_NOT_SPECIFIED);
            return;
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dpaConfPath);
            deployedPackages.store(fos, null);
            fos.flush();
            fos.getFD().sync();
        } catch (IOException e) {
            s_logger.error("Error writing package configuration file", e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                s_logger.error("Exception while closing opened resources!", e);
            }
        }
    }

    private void sendSysUpdateSuccess() throws KuraException {
        s_logger.info("Ready to send success after install");
        File installDir = new File(installPersistanceLocation);
        if (installDir.exists() && installDir.isDirectory()) {
            for (File fileEntry : installDir.listFiles()) {

                if (fileEntry.isFile() && fileEntry.getName().endsWith(PERSISTANCE_SUFFIX)) { // &&
                    // fileEntry.getName().contains(verificationFile.getName()
                    Properties downloadProperties = loadInstallPersistance(fileEntry);
                    String deployUrl = downloadProperties.getProperty(DeploymentDownloadOptions.METRIC_DP_DOWNLOAD_URI);
                    String dpName = downloadProperties.getProperty(DeploymentDownloadOptions.METRIC_DP_NAME);
                    String dpVersion = downloadProperties.getProperty(DeploymentDownloadOptions.METRIC_DP_VERSION);
                    String clientId = downloadProperties.getProperty(DeploymentDownloadOptions.METRIC_DP_CLIENT_ID);
                    Long jobId = Long.valueOf(downloadProperties.getProperty(DeploymentDownloadOptions.METRIC_JOB_ID));
                    String fileSystemFileName = downloadProperties.getProperty(PERSISTANCE_FILE_NAME);
                    String requestClientId = downloadProperties.getProperty(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID);

                    DeploymentDownloadOptions deployOptions = new DeploymentDownloadOptionsImpl(deployUrl, dpName, dpVersion);
                    deployOptions.setClientId(clientId);
                    deployOptions.setJobId(jobId);
                    deployOptions.setRequestClientId(requestClientId);

                    try {
                        installCompleteAsync(deployOptions, fileSystemFileName);
                        s_logger.info("Sent install complete");
                        fileEntry.delete();
                        break;
                    } catch (KuraException e) {
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR);
                    }
                }
            }
        }
    }

    private void sendSysUpdateFailure() throws KuraException {
        File installDir = new File(installPersistanceLocation);
        if (installDir.exists() && installDir.isDirectory()) {
            for (final File fileEntry : installDir.listFiles()) {
                if (fileEntry.isFile() && fileEntry.getName().endsWith(PERSISTANCE_SUFFIX)) { // &&
                    // fileEntry.getName().contains(verificationFile.getName())
                    Properties downloadProperties = loadInstallPersistance(fileEntry);
                    String deployUrl = downloadProperties.getProperty(DeploymentDownloadOptionsImpl.METRIC_DP_DOWNLOAD_URI);
                    String dpName = downloadProperties.getProperty(DeploymentDownloadOptionsImpl.METRIC_DP_NAME);
                    String dpVersion = downloadProperties.getProperty(DeploymentDownloadOptionsImpl.METRIC_DP_VERSION);
                    String clientId = downloadProperties.getProperty(DeploymentDownloadOptionsImpl.METRIC_DP_CLIENT_ID);
                    Long jobId = Long.valueOf(downloadProperties.getProperty(DeploymentDownloadOptionsImpl.METRIC_JOB_ID));
                    String fileSystemFileName = downloadProperties.getProperty(PERSISTANCE_FILE_NAME);
                    String requestClientId = downloadProperties.getProperty(CloudDeploymentHandlerV2.METRIC_REQUESTER_CLIENT_ID);

                    DeploymentDownloadOptionsImpl deployOptions = new DeploymentDownloadOptionsImpl(deployUrl, dpName, dpVersion);
                    deployOptions.setClientId(clientId);
                    deployOptions.setJobId(jobId);
                    deployOptions.setRequestClientId(requestClientId);

                    try {
                        installFailedAsync(deployOptions, fileSystemFileName, new KuraException(KuraErrorCode.INTERNAL_ERROR));
                        s_logger.info("Sent install failed");
                        fileEntry.delete();
                        break;
                    } catch (KuraException e) {
                        throw new KuraException(KuraErrorCode.INTERNAL_ERROR, e);
                    }
                }
            }
        }
    }

    private Properties loadInstallPersistance(File installedDpPersistance) {
        Properties downloadProperies = new Properties();
        FileReader fr = null;
        try {
            fr = new FileReader(installedDpPersistance);
            downloadProperies.load(fr);
        } catch (IOException e) {
            s_logger.error("Exception loading install configuration file", e);
        } finally {
            try {
                if (fr != null) {
                    fr.close();
                }
            } catch (IOException e) {
                s_logger.error("Exception while closing opened resources!", e);
            }
        }
        return downloadProperies;
    }
}
