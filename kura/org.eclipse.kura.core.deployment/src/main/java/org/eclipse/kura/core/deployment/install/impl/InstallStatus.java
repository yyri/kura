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
package org.eclipse.kura.core.deployment.install.impl;

import org.eclipse.kura.core.deployment.DeployStatus;

/**
 * Enum representing the different status of the installation process
 * 
 * {@link InstallStatus.IDLE} Install idle
 * {@link InstallStatus.IN_PROGRESS} Install in progress
 * {@link InstallStatus.COMPLETED} Install completed
 * {@link InstallStatus.FAILED} Install failed
 * {@link InstallStatus.ALREADY_DONE} Install already done
 * {@link InstallStatus.CANCELLED} Install cancelled
 */
public enum InstallStatus implements DeployStatus{
    IDLE("IDLE"), IN_PROGRESS("IN_PROGRESS"), COMPLETED("COMPLETED"), FAILED("FAILED"), ALREADY_DONE("ALREADY DONE");

    private final String status;

    InstallStatus(String status) {
        this.status = status;
    }

    @Override
    public String getStatusString() {
        return status;
    }
}