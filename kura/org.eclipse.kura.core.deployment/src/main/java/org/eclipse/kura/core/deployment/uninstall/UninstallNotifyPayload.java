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
package org.eclipse.kura.core.deployment.uninstall;

public interface UninstallNotifyPayload {

    public static final String METRIC_CLIENT_ID          = "client.id";
    public static final String METRIC_UNINSTALL_PROGRESS = "dp.uninstall.progress";
    public static final String METRIC_UNINSTALL_STATUS   = "dp.uninstall.status";
    public static final String METRIC_DP_NAME            = "dp.name";
    public static final String METRIC_DP_VERSION         = "dp.version";
    public static final String METRIC_JOB_ID             = "job.id";

    public void setClientId(String requesterClientId);

    public String getClientId();

    public void setUninstallProgress(int installProgress);

    public int getUninstallProgress();

    public void setUninstallStatus(String installStatus);

    public int getUninstallStatus();

    public void setDpName(String dpName);

    public void setDpVersion(String dpVersion);

    public void setJobId(long jobId);

    public Long getJobId();

    public void setErrorMessage(String errorMessage);

    public String getErrorMessage();
}
