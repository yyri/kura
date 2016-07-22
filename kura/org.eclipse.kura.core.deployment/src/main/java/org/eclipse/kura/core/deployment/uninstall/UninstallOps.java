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
package org.eclipse.kura.core.deployment.uninstall;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.DeploymentOptionsImpl;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.osgi.service.deploymentadmin.DeploymentAdmin;

public interface UninstallOps {
    public static final String RESOURCE_UNINSTALL = "uninstall";

    public CloudDeploymentHandlerV2 getCallback();

    public void setCallback(CloudDeploymentHandlerV2 callback);

    public DeploymentAdmin getDeploymentAdmin();

    public void setDeploymentAdmin(DeploymentAdmin deploymentAdmin);

    public DeploymentOptionsImpl getOptions();

    public void setOptions(DeploymentOptionsImpl options);

    public boolean isUninstalling();

    public void cleanup();

    public void uninstallFailedAsync(Exception e);

    public void uninstall() throws KuraException;
}
