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

import org.eclipse.kura.core.deployment.install.DeploymentInstallOptions;

public interface DeploymentDownloadOptions extends DeploymentInstallOptions{
    
 // Metrics in RESOURCE_DOWNLOAD
    public static final String METRIC_DP_DOWNLOAD_URI               = "dp.uri";
    public static final String METRIC_DP_DOWNLOAD_PROTOCOL          = "dp.download.protocol";
    public static final String METRIC_DP_DOWNLOAD_BLOCK_SIZE        = "dp.download.block.size";
    public static final String METRIC_DP_DOWNLOAD_BLOCK_DELAY       = "dp.download.block.delay";
    public static final String METRIC_DP_DOWNLOAD_TIMEOUT           = "dp.download.timeout";
    public static final String METRIC_DP_DOWNLOAD_RESUME            = "dp.download.resume";
    public static final String METRIC_DP_DOWNLOAD_USER              = "dp.download.username";
    public static final String METRIC_DP_DOWNLOAD_PASSWORD          = "dp.download.password";
    public static final String METRIC_DP_DOWNLOAD_NOTIFY_BLOCK_SIZE = "dp.download.notify.block.size";
    public static final String METRIC_DP_DOWNLOAD_FORCE_DOWNLOAD    = "dp.download.force";
    public static final String METRIC_DP_DOWNLOAD_HASH              = "dp.download.hash";
    public static final String METRIC_DP_INSTALL                    = "dp.install";

    public int getNotifyBlockSize();

    public void setNotifyBlockSize(int notifyBlockSize);

    public String getDeployUri();

    public void setDeployUri(String deployUri);

    public String getDownloadProtocol();

    public void setDownloadProtocol(String downloadProtocol);

    public int getBlockSize();

    public void setBlockSize(int blockSize);

    public int getBlockDelay();

    public void setBlockDelay(int blockDelay);

    public int getTimeout();

    public void setTimeout(int timeout);

    public boolean isDownloadForced();

    public void setDownloadForced(boolean forceDownload);

    public String getUsername();

    public void setUsername(String username);

    public String getPassword();

    public void setPassword(String password);

    public String getHash();

    public void setHash(String hash);

}
