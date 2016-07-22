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

package org.eclipse.kura.core.deployment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.cloud.Cloudlet;
import org.eclipse.kura.cloud.CloudletTopic;
import org.eclipse.kura.core.deployment.download.DeploymentDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadCountingOutputStream;
import org.eclipse.kura.core.deployment.download.DownloadOps;
import org.eclipse.kura.core.deployment.download.impl.DeploymentDownloadOptionsImpl;
import org.eclipse.kura.core.deployment.download.impl.DownloadFileUtilitiesImpl;
import org.eclipse.kura.core.deployment.download.impl.DownloadOpsImpl;
import org.eclipse.kura.core.deployment.download.impl.DownloadObjectPool;
import org.eclipse.kura.core.deployment.download.impl.DownloadStatusImpl;
import org.eclipse.kura.core.deployment.install.DeploymentInstallOptions;
import org.eclipse.kura.core.deployment.install.impl.DeploymentInstallOptionsImpl;
import org.eclipse.kura.core.deployment.install.impl.InstallOpsImpl;
import org.eclipse.kura.core.deployment.uninstall.impl.DeploymentUninstallOptionsImpl;
import org.eclipse.kura.core.deployment.uninstall.impl.UninstallOpsImpl;
import org.eclipse.kura.core.deployment.xml.XmlBundle;
import org.eclipse.kura.core.deployment.xml.XmlBundleInfo;
import org.eclipse.kura.core.deployment.xml.XmlBundles;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackage;
import org.eclipse.kura.core.deployment.xml.XmlDeploymentPackages;
import org.eclipse.kura.core.deployment.xml.XmlUtil;
import org.eclipse.kura.core.util.ThrowableUtil;
import org.eclipse.kura.data.DataTransportService;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;
import org.eclipse.kura.message.KuraResponsePayload;
import org.eclipse.kura.ssl.SslManagerService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentException;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CloudDeploymentHandlerV2 class extends a Cloudlet and implement the logic
 * for the following operations:
 * <ul>
 * <li><b>Download</b> of an OSGi deployment package (.dp) or Shell script file
 * (.sh)</li>
 * <li><b>Installation</b> of downloaded elements</li>
 * <li><b>Uninstall</b> of deployment package resources running in the framework
 * </li>
 * <li><b>Cancel</b> a running download operation</li>
 * <li><b>Listing</b> of deployment packages running in the Kura framework</li>
 * <li><b>Listing</b> of all the packages running in the Kura framework</li>
 * <li><b>Remote starting</b> of an installed deployment package</li>
 * <li><b>Remote stopping</b> of an installed and running deployment package
 * </li>
 * </ul>
 *
 */
public class CloudDeploymentHandlerV2 extends Cloudlet {
    private static final String ENCODING = "UTF-8";
    private static final Logger s_logger = LoggerFactory.getLogger(CloudDeploymentHandlerV2.class);
    private static final String APP_ID   = "DEPLOY-V2";

    private static final String DPA_CONF_PATH_PROPNAME = "dpa.configuration";
    private static final String KURA_CONF_URL_PROPNAME = "kura.configuration";
    private static final String PACKAGES_PATH_PROPNAME = "kura.packages";
    private static final String KURA_DATA_DIR          = "kura.data";

    public static final String RESOURCE_PACKAGES = "packages";
    public static final String RESOURCE_BUNDLES  = "bundles";

    /* EXEC */
    public static final String RESOURCE_DOWNLOAD  = "download";
    public static final String RESOURCE_INSTALL   = "install";
    public static final String RESOURCE_UNINSTALL = "uninstall";
    public static final String RESOURCE_CANCEL    = "cancel";
    public static final String RESOURCE_START     = "start";
    public static final String RESOURCE_STOP      = "stop";

    /* Metrics in the REPLY to RESOURCE_DOWNLOAD */
    public static final String METRIC_DOWNLOAD_STATUS     = "download.status";
    public static final String METRIC_REQUESTER_CLIENT_ID = "requester.client.id";

    private static ExecutorService executor = Executors.newSingleThreadExecutor();

    private SslManagerService sslManagerService;
    private DeploymentAdmin   deploymentAdmin;

    private Future<?> downloaderFuture;
    private Future<?> installerFuture;

    private BundleContext bundleContext;

    private DataTransportService dataTransportService;

    private String dpaConfPath;
    private String packagesPath;

    private String installVerificationDir;

    private Properties kuraProperties;

    /**
     * Class constructor It invokes the Cloudlet constructor specifying the
     * APP_ID of this application
     */
    public CloudDeploymentHandlerV2() {
        super(APP_ID);
    }

    // ----------------------------------------------------------------
    //
    // Dependencies
    //
    // ----------------------------------------------------------------

    public void setSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = sslManagerService;
    }

    public void unsetSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = null;
    }

    protected void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = deploymentAdmin;
    }

    protected void unsetDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = null;
    }

    public void setDataTransportService(DataTransportService dataTransportService) {
        this.dataTransportService = dataTransportService;
    }

    public void unsetDataTransportService(DataTransportService dataTransportService) {
        this.dataTransportService = null;
    }

    // ----------------------------------------------------------------
    //
    // Activation APIs
    //
    // ----------------------------------------------------------------

    @Override
    protected void activate(ComponentContext componentContext) {
        s_logger.info("Cloud Deployment v2 is starting");
        super.activate(componentContext);

        bundleContext = componentContext.getBundleContext();

        dpaConfPath = System.getProperty(DPA_CONF_PATH_PROPNAME);
        if (dpaConfPath == null || dpaConfPath.isEmpty()) {
            throw new ComponentException("The value of '" + DPA_CONF_PATH_PROPNAME + "' is not defined");
        }

        loadKuraProperties();

        packagesPath = kuraProperties.getProperty(PACKAGES_PATH_PROPNAME);
        if (packagesPath == null || packagesPath.isEmpty()) {
            throw new ComponentException("The value of '" + PACKAGES_PATH_PROPNAME + "' is not defined");
        }
        if (kuraProperties.getProperty(PACKAGES_PATH_PROPNAME) != null &&
                "kura/packages".equals(kuraProperties.getProperty(PACKAGES_PATH_PROPNAME).trim())) {
            kuraProperties.setProperty(PACKAGES_PATH_PROPNAME, "/opt/eclipse/kura/kura/packages");
            packagesPath = kuraProperties.getProperty(PACKAGES_PATH_PROPNAME);
            s_logger.warn("Overridding invalid kura.packages location");
        }

        setInstallContext();
    }

    @Override
    protected void deactivate(ComponentContext componentContext) {
        s_logger.info("Bundle " + APP_ID + " is deactivating!");
        if (downloaderFuture != null) {
            downloaderFuture.cancel(true);
        }

        if (installerFuture != null) {
            installerFuture.cancel(true);
        }

        bundleContext = null;
    }

    // ----------------------------------------------------------------
    //
    // Public methods
    //
    // ----------------------------------------------------------------
    /**
     * Callback method used to publish notification messages
     * 
     * @param options
     *            DeploymentPackageOptions used to retrieve information about
     *            the clientId or the requestClientId and that are necessary for
     *            the construction of the notification topic and message
     * @param messagePayload
     *            the message that has to be sent
     * @param messageType
     *            a String representing the message type of the message that has
     *            to be sent
     */
    public void publishNotificationMessage(DeploymentOptions options, KuraPayload messagePayload, String messageType) {
        try {
            String messageTopic = new StringBuilder("NOTIFY/").append(options.getClientId())
                    .append("/")
                    .append(messageType)
                    .toString();

            getCloudApplicationClient().controlPublish(options.getRequestClientId(), messageTopic, messagePayload, 1, DFLT_RETAIN, DFLT_PRIORITY);
        } catch (KuraException e) {
            s_logger.error("Error publishing response for command {} {}", messageType, e);
        }
    }

    // ----------------------------------------------------------------
    //
    // Protected methods
    //
    // ----------------------------------------------------------------

    @Override
    protected void doGet(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

        String[] resources = reqTopic.getResources();

        if (resources == null || resources.length == 0) {
            s_logger.error("Bad request topic: {}", reqTopic.toString());
            s_logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        if (resources[0].equals(RESOURCE_DOWNLOAD)) {
            doGetDownload(respPayload);
        } else if (resources[0].equals(RESOURCE_INSTALL)) {
            doGetInstall(respPayload);
        } else if (resources[0].equals(RESOURCE_PACKAGES)) {
            doGetPackages(respPayload);
        } else if (resources[0].equals(RESOURCE_BUNDLES)) {
            doGetBundles(respPayload);
        } else {
            s_logger.error("Bad request topic: {}", reqTopic.toString());
            s_logger.error("Cannot find resource with name: {}", resources[0]);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
            return;
        }
    }

    @Override
    protected void doExec(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

        String[] resources = reqTopic.getResources();

        if (resources == null || resources.length == 0) {
            s_logger.error("Bad request topic: {}", reqTopic.toString());
            s_logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        if (resources[0].equals(RESOURCE_DOWNLOAD)) {
            doExecDownload(reqPayload, respPayload);
        } else if (resources[0].equals(RESOURCE_INSTALL)) {
            doExecInstall(reqPayload, respPayload);
        } else if (resources[0].equals(RESOURCE_UNINSTALL)) {
            doExecUninstall(reqPayload, respPayload);
        } else if (resources[0].equals(RESOURCE_START)) {
            String bundleId = resources[1];
            doExecStartStopBundle(respPayload, true, bundleId);
        } else if (resources[0].equals(RESOURCE_STOP)) {
            String bundleId = resources[1];
            doExecStartStopBundle(respPayload, false, bundleId);
        } else {
            s_logger.error("Bad request topic: {}", reqTopic.toString());
            s_logger.error("Cannot find resource with name: {}", resources[0]);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
            return;
        }
    }

    @Override
    protected void doDel(CloudletTopic reqTopic, KuraRequestPayload reqPayload, KuraResponsePayload respPayload) throws KuraException {

        String[] resources = reqTopic.getResources();

        if (resources == null || resources.length == 0) {
            s_logger.error("Bad request topic: {}", reqTopic.toString());
            s_logger.error("Expected one resource but found {}", resources != null ? resources.length : "none");
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            return;
        }

        if (resources[0].equals(RESOURCE_DOWNLOAD)) {
            doDelDownload(respPayload);
        } else {
            s_logger.error("Bad request topic: {}", reqTopic.toString());
            s_logger.error("Cannot find resource with name: {}", resources[0]);
            respPayload.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
            return;
        }
    }

    // ----------------------------------------------------------------
    //
    // Private methods
    //
    // ----------------------------------------------------------------
    private void loadKuraProperties() {
        String sKuraConfUrl = System.getProperty(KURA_CONF_URL_PROPNAME);
        if (sKuraConfUrl == null || sKuraConfUrl.isEmpty()) {
            throw new ComponentException("The value of '" + KURA_CONF_URL_PROPNAME + "' is not defined");
        }

        URL kuraUrl = null;
        try {
            kuraUrl = new URL(sKuraConfUrl);
        } catch (MalformedURLException e) {
            throw new ComponentException("Invalid Kura configuration URL");
        }

        kuraProperties = new Properties();
        try {
            kuraProperties.load(kuraUrl.openStream());
        } catch (FileNotFoundException e) {
            throw new ComponentException("Kura configuration file not found", e);
        } catch (IOException e) {
            throw new ComponentException("Exception loading Kura configuration file", e);
        }
    }

    private void setInstallContext() {
        String kuraDataDir = kuraProperties.getProperty(KURA_DATA_DIR);

        InstallOpsImpl installImplementation = InstallOpsImpl.getInstance();
        installImplementation.initPersistance(kuraDataDir);
        installImplementation.setPackagesPath(packagesPath);
        installImplementation.setDpaConfPath(dpaConfPath);
        installImplementation.setDeploymentAdmin(deploymentAdmin);
        installImplementation.sendInstallConfirmations();
    }

    private void doDelDownload(KuraResponsePayload response) {

        try {
            DownloadOps downloadImplementation = DownloadObjectPool.getInstance();
            DownloadCountingOutputStream downloadHelper = downloadImplementation.getDownloadHelper();
            DeploymentDownloadOptions downloadOptions = downloadImplementation.getDownloadOptions();
            if (downloadHelper != null && downloadOptions != null) {
                downloadHelper.cancelDownload();
                DownloadFileUtilitiesImpl.deleteDownloadedFile(downloadOptions);
            }
        } catch (Exception ex) {
            String errMsg = "Error cancelling download!";
            s_logger.warn(errMsg, ex);
            response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            response.setTimestamp(new Date());
            try {
                response.setBody(errMsg.getBytes(ENCODING));
                response.setException(ex);
            } catch (UnsupportedEncodingException uee) {
                s_logger.warn("Unsupported encoding", uee);
            }
        }

    }

    private void doExecDownload(KuraRequestPayload request, KuraResponsePayload response) {

        // Get a downloadImpl object from the pool
        // Check if a download is in progress. If yes, return with an error
        // response.
        DownloadOps downloadImplementation = DownloadObjectPool.getInstance();
        if (downloadImplementation.isDownloadInProgress()) {
            s_logger.info("Another request is pending.");

            response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            response.setTimestamp(new Date());
            response.addMetric(METRIC_DOWNLOAD_STATUS, DownloadStatusImpl.IN_PROGRESS.getStatusString());
            try {
                response.setBody("Another resource is already in download".getBytes(ENCODING));
            } catch (UnsupportedEncodingException e) {
                s_logger.info("Unsupported encoding", e);
            }
            return;
        }

        // Parse options received from request
        // If options received are not correct/complete an error response will
        // be
        // returned
        DeploymentDownloadOptionsImpl options;
        try {
            options = new DeploymentDownloadOptionsImpl(request);
            options.setClientId(dataTransportService.getClientId());
        } catch (KuraException ex) {
            s_logger.info("Malformed download request!");
            response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            response.setTimestamp(new Date());
            try {
                response.setBody("Malformed download request".getBytes(ENCODING));
            } catch (UnsupportedEncodingException e) {
                s_logger.info("Unsupported encoding", e);
            }
            response.setException(ex);
            return;
        }

        // Check if the requested file is already downloaded
        boolean alreadyDownloaded = false;
        try {
            alreadyDownloaded = DownloadFileUtilitiesImpl.isAlreadyDownloaded(options);
        } catch (KuraException ex) {
            response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            response.setException(ex);
            response.setTimestamp(new Date());
            try {
                response.setBody("Error checking download status".getBytes(ENCODING));
            } catch (UnsupportedEncodingException e) {
                s_logger.info("Unsupported encoding", e);
            }
            return;
        }

        // Everything seems in place. Proceed with the downloading and,
        // eventually, install the received file.
        s_logger.info("About to download and install package at URL {}", options.getDeployUri());
        downloadImplementation.setCallback(this);
        downloadImplementation.setDownloadOptions(options);
        downloadImplementation.setSslManager(sslManagerService);
        downloadImplementation.setAlreadyDownloadedFlag(alreadyDownloaded);
        downloadImplementation.setVerificationDirectory(installVerificationDir);

        downloaderFuture = executor.submit(new DownloadRunnable());

        // The downloading will go and report asynchronously. The method can
        // return immediately
    }

    private void doExecInstall(KuraRequestPayload request, KuraResponsePayload response) {
        DeploymentInstallOptionsImpl options;
        try {
            options = new DeploymentInstallOptionsImpl(request);
            options.setClientId(dataTransportService.getClientId());
        } catch (KuraException ex) {
            s_logger.error("Malformed install request!");
            response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            response.setTimestamp(new Date());
            try {
                response.setBody("Malformed install request".getBytes(ENCODING));
            } catch (UnsupportedEncodingException e) {
                // Ignore
            }
            response.setException(ex);

            return;
        }

        boolean alreadyDownloaded = false;
        try {
            alreadyDownloaded = DownloadFileUtilitiesImpl.isAlreadyDownloaded(options);
        } catch (KuraException ex) {
            response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            response.setException(ex);
            response.setTimestamp(new Date());
            try {
                response.setBody("Error checking download status".getBytes(ENCODING));
            } catch (UnsupportedEncodingException e) {}
            return;
        }

        InstallOpsImpl installImplementation = InstallOpsImpl.getInstance();
        if (alreadyDownloaded && !installImplementation.isInstalling()) {
            installImplementation.setOptions(options);

            // if yes, install
            installerFuture = executor.submit(new Runnable() {

                @Override
                public void run() {
                    InstallOpsImpl installImplementation = InstallOpsImpl.getInstance();
                    DeploymentInstallOptions options = installImplementation.getOptions();
                    File dpFile = null;
                    try {
                        dpFile = DownloadFileUtilitiesImpl.getDownloadFile(options);
                        installDownloadedFile(dpFile, options);
                    } catch (KuraException e) {
                        s_logger.warn("Impossible to send an exception message to the cloud platform", e);
                        if (dpFile != null && !dpFile.delete()) {
                            s_logger.warn("Failed to delete stale file.");
                        }
                    } catch (IOException e) {
                        s_logger.warn("Error while trying to get the file to install", e);
                    } finally {
                        installImplementation.cleanup();
                    }
                }
            });

        } else {
            response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
            response.setException(new KuraException(KuraErrorCode.INTERNAL_ERROR));
            response.setTimestamp(new Date());
            try {
                response.setBody("Already installing/uninstalling".getBytes(ENCODING));
            } catch (UnsupportedEncodingException e) {}
        }
    }

    private void doExecUninstall(KuraRequestPayload request, KuraResponsePayload response) {
        //
        // We only allow one request at a time
        InstallOpsImpl installImplementation = InstallOpsImpl.getInstance();
        UninstallOpsImpl uninstallImpl = UninstallOpsImpl.getInstance();
        if (installImplementation.isInstalling() || uninstallImpl.isUninstalling()) {
            s_logger.info("Another request seems still pending: {}. Checking if stale...");

            response = new KuraResponsePayload(KuraResponsePayload.RESPONSE_CODE_ERROR);
            response.setTimestamp(new Date());
            try {
                response.setBody("Only one request at a time is allowed".getBytes(ENCODING));
            } catch (UnsupportedEncodingException e) {
                // Ignore
            }
        } else {
            DeploymentUninstallOptionsImpl options;
            try {
                options = new DeploymentUninstallOptionsImpl(request);
                options.setClientId(dataTransportService.getClientId());
            } catch (Exception ex) {
                s_logger.error("Malformed uninstall request!");
                response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
                response.setTimestamp(new Date());
                try {
                    response.setBody("Malformed uninstall request".getBytes(ENCODING));
                } catch (UnsupportedEncodingException e) {
                    // Ignore
                }
                response.setException(ex);

                return;
            }

            s_logger.info("About to uninstall package {}", options.getDpName());

            uninstallImpl.setCallback(this);
            uninstallImpl.setOptions(options);
            uninstallImpl.setDeploymentAdmin(deploymentAdmin);

            s_logger.info("Uninstalling package...");
            installerFuture = executor.submit(new Runnable() {

                @Override
                public void run() {
                    UninstallOpsImpl uninstallImplementation = UninstallOpsImpl.getInstance();
                    try {
                        uninstallImplementation.uninstall();
                    } catch (Exception e) {
                        uninstallImplementation.uninstallFailedAsync(e);
                    } finally {
                        uninstallImplementation.cleanup();
                    }
                }
            });
        }
    }

    private void doExecStartStopBundle(KuraResponsePayload response, boolean start, String bundleId) {
        if (bundleId == null) {
            s_logger.info("EXEC start/stop bundle: null bundle ID");
            response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
            response.setTimestamp(new Date());
        } else {
            Long id = null;
            try {
                id = Long.valueOf(bundleId);
            } catch (NumberFormatException e) {
                s_logger.error("EXEC start/stop bundle: bad bundle ID format: {}", e);
                response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_BAD_REQUEST);
                response.setTimestamp(new Date());
                response.setExceptionMessage(e.getMessage());
                response.setExceptionStack(ThrowableUtil.stackTraceAsString(e));
            }

            if (id != null) {
                s_logger.info("Executing command {}", start ? RESOURCE_START : RESOURCE_STOP);

                Bundle bundle = bundleContext.getBundle(id);
                if (bundle == null) {
                    s_logger.error("Bundle ID {} not found", id);
                    response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_NOTFOUND);
                    response.setTimestamp(new Date());
                } else {
                    try {
                        if (start) {
                            bundle.start();
                        } else {
                            bundle.stop();
                        }
                        s_logger.info("{} bundle ID {} ({})", new Object[] { start ? "Started" : "Stopped", id, bundle.getSymbolicName() });
                        response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_OK);
                        response.setTimestamp(new Date());
                    } catch (BundleException e) {
                        s_logger.error("Failed to {} bundle {}: {}", new Object[] { start ? "start" : "stop", id, e });
                        response.setResponseCode(KuraResponsePayload.RESPONSE_CODE_ERROR);
                        response.setTimestamp(new Date());
                    }
                }
            }
        }
    }

    private void doGetInstall(KuraResponsePayload respPayload) {
        InstallOpsImpl installImplementation = InstallOpsImpl.getInstance();
        if (installImplementation.isInstalling()) {
            installImplementation.installInProgressSyncMessage(respPayload);
        } else {
            installImplementation.installIdleSyncMessage(respPayload);
        }
    }

    private void doGetDownload(KuraResponsePayload respPayload) {
        DownloadOps downloadOps = DownloadObjectPool.getInstance();
        if (downloadOps.isDownloadInProgress() && downloadOps.getDownloadOptions() != null) { // A download is pending
            DownloadCountingOutputStream downloadHelper = downloadOps.getDownloadHelper();
            DownloadOpsImpl.downloadInProgressSyncMessage(respPayload, downloadHelper, downloadOps.getDownloadOptions());
        } else { // No pending downloads
            DownloadOpsImpl.downloadAlreadyDoneSyncMessage(respPayload);
        }
    }

    private void doGetPackages(KuraResponsePayload response) {
        DeploymentPackage[] dps = deploymentAdmin.listDeploymentPackages();
        XmlDeploymentPackages xdps = new XmlDeploymentPackages();
        XmlDeploymentPackage[] axdp = new XmlDeploymentPackage[dps.length];

        for (int i = 0; i < dps.length; i++) {
            DeploymentPackage dp = dps[i];

            XmlDeploymentPackage xdp = new XmlDeploymentPackage();
            xdp.setName(dp.getName());
            xdp.setVersion(dp.getVersion().toString());

            BundleInfo[] bis = dp.getBundleInfos();
            XmlBundleInfo[] axbi = new XmlBundleInfo[bis.length];

            for (int j = 0; j < bis.length; j++) {

                BundleInfo bi = bis[j];
                XmlBundleInfo xbi = new XmlBundleInfo();
                xbi.setName(bi.getSymbolicName());
                xbi.setVersion(bi.getVersion().toString());

                axbi[j] = xbi;
            }

            xdp.setBundleInfos(axbi);
            axdp[i] = xdp;
        }

        xdps.setDeploymentPackages(axdp);

        try {
            String s = XmlUtil.marshal(xdps);
            response.setTimestamp(new Date());
            response.setBody(s.getBytes(ENCODING));
        } catch (Exception e) {
            s_logger.error("Error getting resource {}: {}", RESOURCE_PACKAGES, e);
        }
    }

    private void doGetBundles(KuraResponsePayload response) {
        Bundle[] bundles = bundleContext.getBundles();
        XmlBundles xmlBundles = new XmlBundles();
        XmlBundle[] axb = new XmlBundle[bundles.length];

        for (int i = 0; i < bundles.length; i++) {

            Bundle bundle = bundles[i];
            XmlBundle xmlBundle = new XmlBundle();

            xmlBundle.setName(bundle.getSymbolicName());
            xmlBundle.setVersion(bundle.getVersion().toString());
            xmlBundle.setId(bundle.getBundleId());

            int state = bundle.getState();

            switch (state) {
                case Bundle.UNINSTALLED:
                    xmlBundle.setState("UNINSTALLED");
                    break;

                case Bundle.INSTALLED:
                    xmlBundle.setState("INSTALLED");
                    break;

                case Bundle.RESOLVED:
                    xmlBundle.setState("RESOLVED");
                    break;

                case Bundle.STARTING:
                    xmlBundle.setState("STARTING");
                    break;

                case Bundle.STOPPING:
                    xmlBundle.setState("STOPPING");
                    break;

                case Bundle.ACTIVE:
                    xmlBundle.setState("ACTIVE");
                    break;

                default:
                    xmlBundle.setState(String.valueOf(state));
            }

            axb[i] = xmlBundle;
        }

        xmlBundles.setBundles(axb);

        try {
            String s = XmlUtil.marshal(xmlBundles);
            response.setTimestamp(new Date());
            response.setBody(s.getBytes(ENCODING));
        } catch (Exception e) {
            s_logger.error("Error getting resource {}: {}", RESOURCE_BUNDLES, e);
        }
    }

    private void installDownloadedFile(File dpFile, DeploymentInstallOptions options) throws KuraException {
        InstallOpsImpl installImplementation = InstallOpsImpl.getInstance();
        try {
            if (options.getSystemUpdate()) {
                installImplementation.installSh(options, dpFile);
            } else {
                installImplementation.installDp(options, dpFile);
            }
        } catch (Exception e) {
            s_logger.info("Install exception");
            installImplementation.installFailedAsync(options, dpFile.getName(), e);
        } finally {
            installImplementation.cleanup();
        }
    }

    /**
     * Internal Download runnable class
     *
     */
    private final class DownloadRunnable implements Runnable {
        @Override
        public void run() {
            DownloadOps downloadImplementation = DownloadObjectPool.getInstance();
            try {
                DeploymentDownloadOptions downloadOptions = downloadImplementation.getDownloadOptions();
                if (downloadOptions != null) {
                    File downloadedFile = downloadImplementation.downloadDeploymentPackageInternal();
                    if (downloadedFile != null && downloadOptions.isInstall()) {
                        s_logger.info("Ready to install");
                        installDownloadedFile(downloadedFile, downloadOptions);
                    }
                }
            } catch (KuraException e) {
                s_logger.warn("Exec download failed. Deleting stale objects...", e);
                deleteStaleObjects(downloadImplementation);
            } finally {
                DownloadObjectPool.returnObject(downloadImplementation);
            }
        }

        private void deleteStaleObjects(DownloadOps downloadImplementation) {
            try {
                File dpFile = DownloadFileUtilitiesImpl.getDownloadFile(downloadImplementation.getDownloadOptions());
                if (dpFile != null && !dpFile.delete()) {
                    s_logger.warn("Failed to delete stale object.");
                }
            } catch (IOException e) {
                s_logger.warn("Failed to get the file to delete", e);
            }
        }
    }
}
