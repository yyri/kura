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

import org.eclipse.kura.core.deployment.DeploymentOptions;

public interface DeploymentInstallOptions extends DeploymentOptions {
    
    public static final String METRIC_DP_INSTALL_SYSTEM_UPDATE = "dp.install.system.update";
    public static final String METRIC_INSTALL_VERIFIER_URI     = "dp.install.verifier.uri";
    
    public void setSystemUpdate(Boolean systemUpdate);

    public Boolean getSystemUpdate();

    public void setVerifierURI(String verifierURI);

    public String getVerifierURL();

}
