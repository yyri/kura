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

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.output.CountingOutputStream;
import org.eclipse.kura.core.deployment.download.DeploymentDownloadOptions;
import org.eclipse.kura.core.deployment.progress.ProgressListener;
import org.eclipse.kura.core.deployment.progress.impl.ProgressEventImpl;
import org.eclipse.kura.ssl.SslManagerService;

public class GenericDownloadCountingOutputStream extends CountingOutputStream {

    long totalBytes;

    final DeploymentDownloadOptions options;
    final SslManagerService                sslManagerService;
    final ProgressListener                 pl;
    final int                              alreadyDownloaded;
    final String                           downloadURL;

    InputStream is = null;

    private int            propResolution;
    private int            propBufferSize;
    private int            propConnectTimeout = 5000;
    private int            propReadTimeout    = 6000;
    private int            propBlockDelay     = 1000;
    private long           currentStep        = 1;
    private DownloadStatusImpl downloadStatus     = DownloadStatusImpl.FAILED;

    public GenericDownloadCountingOutputStream(DownloadOptionsImpl downloadOptions) {
        super(downloadOptions.getOutputStream());
        this.options = downloadOptions.getRequestOptions();
        this.sslManagerService = downloadOptions.getSslManagerService();
        this.pl = downloadOptions.getCallback();
        this.downloadURL = downloadOptions.getDownloadURL();
        this.alreadyDownloaded = downloadOptions.getAlreadyDownloaded();
    }

    public DownloadStatusImpl getDownloadTransferStatus() {
        return downloadStatus;
    }

    public Long getDownloadTransferProgressPercentage() {
        Long percentage = (long) Math.floor((((Long) getByteCount()).doubleValue() / ((Long) totalBytes).doubleValue()) * 100);
        if (percentage < 0) {
            return (long) 50;
        }
        return percentage;
    }

    public Long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    @Override
    protected void afterWrite(int n) throws IOException {
        super.afterWrite(n);
        if (propResolution == 0 && getTotalBytes() > 0) {
            propResolution = Math.round((totalBytes / 100) * 5);
        } else if (propResolution == 0) {
            propResolution = 1024 * 256;
        }
        if (getByteCount() >= currentStep * propResolution) {
            currentStep++;
            postProgressEvent(totalBytes, DownloadStatusImpl.IN_PROGRESS, null);
        }
        try {
            Thread.sleep(propBlockDelay);
        } catch (InterruptedException e) {
        }
    }

    protected void postProgressEvent(long total, DownloadStatusImpl status, String errorMessage) {
        Long perc = getDownloadTransferProgressPercentage();
        downloadStatus = status;
        ProgressEventImpl pe = new ProgressEventImpl(this, options, ((Long) total).intValue(), perc.intValue(),
                getDownloadTransferStatus().getStatusString(), alreadyDownloaded);
        if (errorMessage != null) {
            pe.setExceptionMessage(errorMessage);
        }
        pl.progressChanged(pe);

    }

    protected void setResolution(int resolution) {
        propResolution = resolution;
    }

    protected void setBufferSize(int size) {
        propBufferSize = size;
    }

    protected void setConnectTimeout(int timeout) {
        propConnectTimeout = timeout;
    }

    protected void setReadTimeout(int timeout) {
        propReadTimeout = timeout;
    }

    protected void setBlockDelay(int delay) {
        propBlockDelay = delay;
    }

    protected int getResolution() {
        return propResolution;
    }

    protected int getBufferSize() {
        return propBufferSize;
    }

    protected int getConnectTimeout() {
        return propConnectTimeout;
    }

    protected int getPropReadTimeout() {
        return propReadTimeout;
    }

    protected int getPropBlockDelay() {
        return propBlockDelay;
    }
}
