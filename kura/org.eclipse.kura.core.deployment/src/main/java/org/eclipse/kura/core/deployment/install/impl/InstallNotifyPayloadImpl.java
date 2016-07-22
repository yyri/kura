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

import java.text.ParseException;

import org.eclipse.kura.core.deployment.download.impl.DownloadNotifyPayloadImpl;
import org.eclipse.kura.core.deployment.install.InstallNotifyPayload;
import org.eclipse.kura.message.KuraPayload;

public class InstallNotifyPayloadImpl extends KuraPayload implements InstallNotifyPayload{

    public InstallNotifyPayloadImpl(String clientId) {
        super();
        addMetric(METRIC_CLIENT_ID, clientId);
    }

    public InstallNotifyPayloadImpl(KuraPayload kuraPayload) {
        for (String name : kuraPayload.metricNames()) {
            Object value = kuraPayload.getMetric(name);
            addMetric(name, value);
        }
        setBody(kuraPayload.getBody());
        setPosition(kuraPayload.getPosition());
        setTimestamp(kuraPayload.getTimestamp());
    }

    @Override
    public void setClientId(String requesterClientId) {
        addMetric(METRIC_CLIENT_ID, requesterClientId);
    }

    @Override
    public String getClientId() {
        return (String) getMetric(METRIC_CLIENT_ID);
    }

    @Override
    public void setInstallProgress(int installProgress) {
        addMetric(METRIC_INSTALL_PROGRESS, installProgress);
    }

    @Override
    public int getInstallProgress() {
        return (Integer) getMetric(METRIC_INSTALL_PROGRESS);
    }

    @Override
    public void setInstallStatus(String installStatus) {
        addMetric(METRIC_INSTALL_STATUS, installStatus);
    }

    @Override
    public int getInstallStatus() {
        return (Integer) getMetric(METRIC_INSTALL_STATUS);
    }

    @Override
    public void setDpName(String dpName) {
        addMetric(METRIC_DP_NAME, dpName);
    }

    @Override
    public void setDpVersion(String dpVersion) {
        addMetric(METRIC_DP_VERSION, dpVersion);
    }

    @Override
    public void setJobId(long jobId) {
        addMetric(METRIC_JOB_ID, jobId);
    }

    @Override
    public Long getJobId() {
        return (Long) getMetric(METRIC_JOB_ID);
    }

    @Override
    public void setErrorMessage(String errorMessage) {
        addMetric(METRIC_ERROR_MESSAGE, errorMessage);
    }

    @Override
    public String getErrorMessage() {
        return (String) getMetric(METRIC_ERROR_MESSAGE);
    }

    public static DownloadNotifyPayloadImpl buildFromKuraPayload(KuraPayload payload) throws ParseException {
        if (payload.getMetric(METRIC_CLIENT_ID) == null) {
            throw new ParseException("Not a valid notify payload", 0);
        }
        
        return new DownloadNotifyPayloadImpl(payload);
    }
}