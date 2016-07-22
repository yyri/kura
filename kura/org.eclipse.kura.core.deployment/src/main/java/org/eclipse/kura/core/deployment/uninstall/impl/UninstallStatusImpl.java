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

import org.eclipse.kura.core.deployment.DeployStatus;
import org.eclipse.kura.core.deployment.download.impl.DownloadStatusImpl;

/**
 * Enum representing the different status of the uninstall process
 * 
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.IDLE} Uninstall is in idle state
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.IN_PROGRESS} Uninstall in progress
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.COMPLETED} Uninstall completed
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.FAILED} Uninstall failed
 * {@link DownloadStatusImpl.DOWNLOAD_STATUS.ALREADY_DONE} Uninstall already done
 */
public enum UninstallStatusImpl implements DeployStatus{
    IDLE("IDLE"), IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE");

    private final String status;

    UninstallStatusImpl(String status) {
        this.status = status;
    }

    @Override
    public String getStatusString() {
        return status;
    }
}