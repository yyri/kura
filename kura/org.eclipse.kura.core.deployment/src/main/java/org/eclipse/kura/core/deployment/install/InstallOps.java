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
package org.eclipse.kura.core.deployment.install;

import java.io.File;
import java.util.Properties;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.core.deployment.DeploymentOptions;
import org.eclipse.kura.message.KuraResponsePayload;
import org.osgi.service.deploymentadmin.DeploymentAdmin;

public interface InstallOps {

    public static final String RESOURCE_INSTALL = "install";

    public static final String PERSISTANCE_SUFFIX                   = "_persistance";
    public static final String PERSISTANCE_FOLDER_NAME              = "persistance";
    public static final String PERSISTANCE_VERIFICATION_FOLDER_NAME = "verification";
    public static final String PERSISTANCE_FILE_NAME                = "persistance.file.name";

    public void cleanup();

    public void initPersistance(String kuraDataDir);

    public void setCallback(CloudDeploymentHandlerV2 callback);

    public boolean isInstalling();

    public Properties getDeployedPackages();

    public void setOptions(DeploymentInstallOptions options);

    public DeploymentInstallOptions getOptions();

    public void setPackagesPath(String packagesPath);

    public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin);

    public void setDpaConfPath(String dpaConfPath);

    public void installDp(DeploymentInstallOptions options, File dpFile) throws KuraException;

    public void installSh(DeploymentOptions options, File shFile) throws KuraException;

    public void installInProgressSyncMessage(KuraResponsePayload respPayload);

    public void installIdleSyncMessage(KuraResponsePayload respPayload);

    public void installCompleteAsync(DeploymentOptions options, String dpName) throws KuraException;

    public void installFailedAsync(DeploymentInstallOptions options, String dpName, Exception e) throws KuraException;

    public void sendInstallConfirmations();

    public void removePackageFromConfFile(String packageName);
}
