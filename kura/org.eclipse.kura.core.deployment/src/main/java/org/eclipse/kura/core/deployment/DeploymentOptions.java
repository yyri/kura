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
package org.eclipse.kura.core.deployment;

/**
 * The DeploymentOptions is an interface for all the download, install and
 * uninstall implementations that extend this interface adding more specific
 * configuration options
 *
 */
public interface DeploymentOptions {

    // Metrics
    public static final String METRIC_DP_NAME         = "dp.name";
    public static final String METRIC_DP_VERSION      = "dp.version";
    public static final String METRIC_DP_REBOOT       = "dp.reboot";
    public static final String METRIC_DP_REBOOT_DELAY = "dp.reboot.delay";
    public static final String METRIC_DP_CLIENT_ID    = "client.id";
    public static final String METRIC_JOB_ID          = "job.id";

    /**
     * Returns the name of the resource to download
     * @return String that represents the name of the resource to download
     */
    public String getDpName();

    /**
     * Sets the name of the resource to download
     * @param dpName a String that represents the name of the resource to download
     */
    public void setDpName(String dpName);

    /**
     * Returns the version of the resource to download
     * @return String that represents the version of the resource to download
     */
    public String getDpVersion();

    /**
     * Sets the version of the resource to download
     * @param dpVersion
     */
    public void setDpVersion(String dpVersion);

    /**
     * 
     * @return
     */
    public Long getJobId();

    /**
     * 
     * @param jobId
     */
    public void setJobId(long jobId);

    /**
     * 
     * @return
     */
    public boolean isResume();

    /**
     * 
     * @param resume
     */
    public void setResume(boolean resume);

    /**
     * 
     * @return
     */
    public boolean isInstall();

    /**
     * 
     * @param install
     */
    public void setInstall(boolean install);

    /**
     * 
     * @return
     */
    public boolean isPostInst();

    /**
     * 
     * @param postInst
     */
    public void setPostInst(boolean postInst);

    /**
     * 
     * @return
     */
    public boolean isDelete();

    /**
     * 
     * @param delete
     */
    public void setDelete(boolean delete);

    /**
     * 
     * @return
     */
    public boolean isReboot();

    /**
     * 
     * @param reboot
     */
    public void setReboot(boolean reboot);

    /**
     * 
     * @return
     */
    public int getRebootDelay();

    /**
     * 
     * @param rebootDelay
     */
    public void setRebootDelay(int rebootDelay);

    /**
     * 
     * @return
     */
    public String getClientId();

    /**
     * 
     * @param clientId
     */
    public void setClientId(String clientId);

    /**
     * 
     * @return
     */
    public String getRequestClientId();

    /**
     * 
     * @param requestClientId
     */
    public void setRequestClientId(String requestClientId);

}
