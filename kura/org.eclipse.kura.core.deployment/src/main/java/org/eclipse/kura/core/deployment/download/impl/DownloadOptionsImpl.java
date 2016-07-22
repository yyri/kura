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

import java.io.OutputStream;

import org.eclipse.kura.core.deployment.download.DeploymentDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadOptions;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.ssl.SslManagerService;

public class DownloadOptionsImpl implements DownloadOptions{
    private OutputStream                         out;
    private DeploymentDownloadOptions options;
    private ProgressListener                     callback;
    private SslManagerService                    sslManagerService;
    private String                               downloadURL;
    private int                                  alreadyDownloaded;

    @Override
    public OutputStream getOutputStream() {
        return out;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public DeploymentDownloadOptions getRequestOptions() {
        return options;
    }

    @Override
    public void setRequestOptions(DeploymentDownloadOptions options) {
        this.options = options;
    }

    @Override
    public ProgressListener getCallback() {
        return callback;
    }

    @Override
    public void setCallback(ProgressListener callback) {
        this.callback = callback;
    }

    @Override
    public SslManagerService getSslManagerService() {
        return sslManagerService;
    }

    @Override
    public void setSslManagerService(SslManagerService sslManagerService) {
        this.sslManagerService = sslManagerService;
    }

    @Override
    public String getDownloadURL() {
        return downloadURL;
    }

    @Override
    public void setDownloadURL(String downloadURL) {
        this.downloadURL = downloadURL;
    }

    @Override
    public int getAlreadyDownloaded() {
        return alreadyDownloaded;
    }

    @Override
    public void setAlreadyDownloaded(int alreadyDownloaded) {
        this.alreadyDownloaded = alreadyDownloaded;
    }
}
