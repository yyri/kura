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

public class DeploymentOptionsImpl implements DeploymentOptions {

    private String  dpName;
    private String  dpVersion;
    private boolean resume      = false;
    private boolean install     = true;
    private boolean postInst    = false;
    private boolean delete      = false;
    private boolean reboot      = false;
    private int     rebootDelay = 0;

    private String clientId        = "";
    private String requestClientId = "";
    private Long   jobId           = null;

    public DeploymentOptionsImpl(String dpName, String dpVersion) {
        this.dpName = dpName;
        this.dpVersion = dpVersion;
    }

    @Override
    public String getDpName() {
        return dpName;
    }

    @Override
    public void setDpName(String dpName) {
        this.dpName = dpName;
    }

    @Override
    public String getDpVersion() {
        return dpVersion;
    }

    @Override
    public void setDpVersion(String dpVersion) {
        this.dpVersion = dpVersion;
    }

    @Override
    public Long getJobId() {
        return jobId;
    }

    @Override
    public void setJobId(long jobId) {
        this.jobId = jobId;
    }

    @Override
    public boolean isResume() {
        return resume;
    }

    @Override
    public void setResume(boolean resume) {
        this.resume = resume;
    }

    @Override
    public boolean isInstall() {
        return install;
    }

    @Override
    public void setInstall(boolean install) {
        this.install = install;
    }

    @Override
    public boolean isPostInst() {
        return postInst;
    }

    @Override
    public void setPostInst(boolean postInst) {
        this.postInst = postInst;
    }

    @Override
    public boolean isDelete() {
        return delete;
    }

    @Override
    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    @Override
    public boolean isReboot() {
        return reboot;
    }

    @Override
    public void setReboot(boolean reboot) {
        this.reboot = reboot;
    }

    @Override
    public int getRebootDelay() {
        return rebootDelay;
    }

    @Override
    public void setRebootDelay(int rebootDelay) {
        this.rebootDelay = rebootDelay;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public String getRequestClientId() {
        return requestClientId;
    }

    @Override
    public void setRequestClientId(String requestClientId) {
        this.requestClientId = requestClientId;
    }
}
