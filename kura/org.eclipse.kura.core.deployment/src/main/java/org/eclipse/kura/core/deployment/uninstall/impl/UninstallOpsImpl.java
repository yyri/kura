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
package org.eclipse.kura.core.deployment.uninstall.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.install.impl.InstallOpsImpl;
import org.eclipse.kura.core.deployment.uninstall.UninstallOps;
import org.eclipse.kura.core.deployment.DeploymentOptionsImpl;
import org.eclipse.kura.core.util.ProcessUtil;
import org.eclipse.kura.core.util.SafeProcess;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UninstallOpsImpl implements UninstallOps {
    private static final Logger s_logger = LoggerFactory.getLogger(UninstallOpsImpl.class);

    public static final UninstallOpsImpl instance = new UninstallOpsImpl();

    private DeploymentOptionsImpl options;
    private CloudDeploymentHandlerV2  callback;
    private DeploymentAdmin           deploymentAdmin;

    public static UninstallOpsImpl getInstance() {
        return instance;
    }

    @Override
    public CloudDeploymentHandlerV2 getCallback() {
        return callback;
    }

    @Override
    public void setCallback(CloudDeploymentHandlerV2 callback) {
        this.callback = callback;
    }

    @Override
    public DeploymentAdmin getDeploymentAdmin() {
        return deploymentAdmin;
    }

    @Override
    public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin) {
        this.deploymentAdmin = deploymentAdmin;
    }

    @Override
    public DeploymentOptionsImpl getOptions() {
        return options;
    }

    @Override
    public void setOptions(DeploymentOptionsImpl options) {
        this.options = options;
    }

    @Override
    public boolean isUninstalling() {
        return options != null || callback != null;
    }

    @Override
    public void cleanup() {
        options = null;
        callback = null;
    }

    @Override
    public void uninstallFailedAsync(Exception e) {
        UninstallNotifyPayloadImpl notify = new UninstallNotifyPayloadImpl(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setUninstallStatus(UninstallStatusImpl.FAILED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setDpName(options.getDpName());
        notify.setUninstallProgress(0);
        if (e != null) {
            notify.setErrorMessage(e.getMessage());
        }
        callback.publishNotificationMessage(options, notify, RESOURCE_UNINSTALL);
    }

    @Override
    public void uninstall() throws KuraException {
        try {
            String name = options.getDpName();
            if (name != null) {
                DeploymentPackage dp = deploymentAdmin.getDeploymentPackage(name);
                if (dp != null) {
                    dp.uninstall();
                    String sUrl = InstallOpsImpl.getInstance().getDeployedPackages().getProperty(name);
                    File dpFile = new File(new URL(sUrl).getPath());
                    if (!dpFile.delete()) {
                        s_logger.warn("Cannot delete file at URL: {}", sUrl);
                    }
                    InstallOpsImpl.getInstance().removePackageFromConfFile(name);
                }
                uninstallCompleteAsync();

                // Reboot?
                deviceReboot();
            }
        } catch (IOException e) {
            throw KuraException.internalError(e);
        } catch (InterruptedException e) {
            throw KuraException.internalError(e);
        } catch (DeploymentException e) {
            throw KuraException.internalError(e);
        }
    }

    private void uninstallCompleteAsync() {
        UninstallNotifyPayloadImpl notify = new UninstallNotifyPayloadImpl(options.getClientId());
        notify.setTimestamp(new Date());
        notify.setUninstallStatus(UninstallStatusImpl.COMPLETED.getStatusString());
        notify.setJobId(options.getJobId());
        notify.setDpName(options.getDpName());
        notify.setUninstallProgress(100);

        callback.publishNotificationMessage(options, notify, RESOURCE_UNINSTALL);
    }

    private void deviceReboot() throws IOException, InterruptedException {
        if (options.isReboot()) {
            s_logger.info("Reboot requested...");
            SafeProcess proc = null;
            try {
                int delay = options.getRebootDelay();
                s_logger.info("Sleeping for {} ms.", delay);
                Thread.sleep(delay);
                s_logger.info("Rebooting...");
                proc = ProcessUtil.exec("reboot");
            } catch (IOException e) {
                s_logger.info("Rebooting... Failure!", e);
                throw e;
            } catch (InterruptedException e) {
                s_logger.info("Rebooting... Failure!", e);
                throw e;
            } finally {
                if (proc != null) {
                    ProcessUtil.destroy(proc);
                }
            }
        }
    }
}
