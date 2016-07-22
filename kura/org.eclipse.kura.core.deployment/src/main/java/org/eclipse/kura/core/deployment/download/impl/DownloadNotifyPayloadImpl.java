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

import java.text.ParseException;

import org.eclipse.kura.core.deployment.download.DownloadNotifyPayload;
import org.eclipse.kura.message.KuraPayload;

public class DownloadNotifyPayloadImpl extends KuraPayload implements DownloadNotifyPayload{

    public DownloadNotifyPayloadImpl(String clientId) {
        super();
        addMetric(METRIC_CLIENT_ID, clientId);
    }

    public DownloadNotifyPayloadImpl(KuraPayload kuraPayload) {
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
    public void setTransferSize(int trasnferSize) {
        addMetric(METRIC_TRANSFER_SIZE, trasnferSize);
    }

    @Override
    public int getTransferSize() {
        return (Integer) getMetric(METRIC_TRANSFER_SIZE);
    }

    @Override
    public void setTransferProgress(int transferProgress) {
        addMetric(METRIC_TRANSFER_PROGRESS, transferProgress);
    }

    @Override
    public int getTransferProgress() {
        return (Integer) getMetric(METRIC_TRANSFER_PROGRESS);
    }

    @Override
    public void setTransferStatus(String transferStatus) {
        addMetric(METRIC_TRANSFER_STATUS, transferStatus);
    }

    @Override
    public int getTransferStatus() {
        return (Integer) getMetric(METRIC_TRANSFER_STATUS);
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

    @Override
    public void setTransferIndex(int transferIndex) {
        addMetric(METRIC_TRANSFER_INDEX, transferIndex);
    }

    @Override
    public Integer getMissingDownloads() {
        return (Integer) getMetric(METRIC_TRANSFER_INDEX);
    }

    public static DownloadNotifyPayloadImpl buildFromKuraPayload(KuraPayload payload) throws ParseException {
        if (payload.getMetric(METRIC_CLIENT_ID) == null) {
            throw new ParseException("Not a valid notify payload", 0);
        }
        return new DownloadNotifyPayloadImpl(payload);
    }
}
