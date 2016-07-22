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

public interface InstallNotifyPayload {

    public static final String METRIC_CLIENT_ID        = "client.id";
    public static final String METRIC_INSTALL_PROGRESS = "dp.install.progress";
    public static final String METRIC_INSTALL_STATUS   = "dp.install.status";
    public static final String METRIC_DP_NAME          = "dp.name";
    public static final String METRIC_DP_VERSION       = "dp.version";
    public static final String METRIC_JOB_ID           = "job.id";
    public static final String METRIC_ERROR_MESSAGE    = "dp.install.error.message";

    public void setClientId(String requesterClientId);

    public String getClientId();

    public void setInstallProgress(int installProgress);

    public int getInstallProgress();

    public void setInstallStatus(String installStatus);

    public int getInstallStatus();

    public void setDpName(String dpName);

    public void setDpVersion(String dpVersion);

    public void setJobId(long jobId);

    public Long getJobId();

    public void setErrorMessage(String errorMessage);

    public String getErrorMessage();
}
