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

package org.eclipse.kura.core.deployment.progress.impl;

import java.util.EventObject;

import org.eclipse.kura.core.deployment.download.DeploymentDownloadOptions;
import org.eclipse.kura.core.deployment.progress.ProgressEvent;

public class ProgressEventImpl extends EventObject implements ProgressEvent {

    /**
     * 
     */
    private static final long serialVersionUID = -4316652505853478843L;

    private String clientId;
    private int    transferSize;
    private int    transferProgress;
    private String transferStatus;
    private String requesterClientId;
    private long   jobId;
    private String exceptionMessage = null;
    private int    downloadIndex;

    public ProgressEventImpl(Object source, DeploymentDownloadOptions options, int transferSize, int transferProgress, String trasnferStatus, int downloadIndex) {
        super(source);
        this.clientId = options.getClientId();
        this.transferSize = transferSize;
        this.transferProgress = transferProgress;
        this.transferStatus = trasnferStatus;
        this.requesterClientId = options.getRequestClientId();
        this.jobId = options.getJobId();
        this.downloadIndex = downloadIndex;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public int getTransferSize() {
        return transferSize;
    }

    @Override
    public int getTransferProgress() {
        return transferProgress;
    }

    @Override
    public String getTransferStatus() {
        return transferStatus;
    }

    @Override
    public String getRequesterClientId() {
        return requesterClientId;
    }

    @Override
    public long getJobId() {
        return jobId;
    }

    @Override
    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    @Override
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    @Override
    public void setDownloadIndex(int downloadIndex) {
        this.downloadIndex = downloadIndex;
    }

    @Override
    public int getDownloadIndex() {
        return downloadIndex;
    }
}
