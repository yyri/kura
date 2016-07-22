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

import org.eclipse.kura.core.deployment.DeployStatus;

/**
 * Enum representing the different status of the download process
 * 
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.IN_PROGRESS} Download in progress
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.COMPLETED} Download completed
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.FAILED} Download failed
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.ALREADY_DONE} Download already done
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.CANCELLED} Download cancelled
 */
public enum DownloadStatusImpl implements DeployStatus{
    IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE"), CANCELLED("CANCELLED");

    private final String status;

    DownloadStatusImpl(String status) {
        this.status = status;
    }

    @Override
    public String getStatusString() {
        return status;
    }
}