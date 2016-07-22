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

import java.io.OutputStream;

import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;

public interface DownloadOptions {

    public OutputStream getOutputStream();

    public void setOutputStream(OutputStream out);

    public DeploymentDownloadOptions getRequestOptions();

    public void setRequestOptions(DeploymentDownloadOptions options);

    public ProgressListener getCallback();

    public void setCallback(ProgressListener callback);

    public SslManagerService getSslManagerService();

    public void setSslManagerService(SslManagerService sslManagerService);

    public String getDownloadURL();

    public void setDownloadURL(String downloadURL);

    public int getAlreadyDownloaded();

    public void setAlreadyDownloaded(int alreadyDownloaded);

}
