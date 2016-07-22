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
package org.eclipse.kura.core.deployment.download;

public interface DownloadNotifyPayload {
    
    public static final String METRIC_CLIENT_ID         = "client.id";
    public static final String METRIC_TRANSFER_SIZE     = "dp.download.size";
    public static final String METRIC_TRANSFER_PROGRESS = "dp.download.progress";
    public static final String METRIC_TRANSFER_STATUS   = "dp.download.status";
    public static final String METRIC_JOB_ID            = "job.id";
    public static final String METRIC_ERROR_MESSAGE     = "dp.download.error.message";
    public static final String METRIC_TRANSFER_INDEX    = "dp.download.index";
    
    public void setClientId(String requesterClientId); 

    public String getClientId(); 

    public void setTransferSize(int trasnferSize); 

    public int getTransferSize(); 

    public void setTransferProgress(int transferProgress); 

    public int getTransferProgress(); 

    public void setTransferStatus(String transferStatus); 

    public int getTransferStatus(); 

    public void setJobId(long jobId); 

    public Long getJobId(); 

    public void setErrorMessage(String errorMessage); 

    public String getErrorMessage(); 

    public void setTransferIndex(int transferIndex); 

    public Integer getMissingDownloads();
}
