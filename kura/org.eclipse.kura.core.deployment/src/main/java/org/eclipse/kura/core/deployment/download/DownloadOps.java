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
package org.eclipse.kura.core.deployment.download;

import java.io.File;

import org.eclipse.kura.KuraException;
import org.eclipse.kura.core.deployment.CloudDeploymentHandlerV2;
import org.eclipse.kura.ssl.SslManagerService;

public interface DownloadOps {

    public DeploymentDownloadOptions getDownloadOptions();

    public boolean isDownloadInProgress();

    public void setDownloadOptions(DeploymentDownloadOptions options);

    public void setCallback(CloudDeploymentHandlerV2 callback);

    public DownloadCountingOutputStream getDownloadHelper();

    public void setSslManager(SslManagerService sslManager);

    public void setAlreadyDownloadedFlag(boolean alreadyDownloaded);

    public void setVerificationDirectory(String verificationDirectory);

    public File downloadDeploymentPackageInternal() throws KuraException;
}
