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

import org.eclipse.kura.KuraErrorCode;
import org.eclipse.kura.KuraException;
import org.eclipse.kura.KuraInvalidMessageException;
import org.eclipse.kura.core.deployment.download.DeploymentDownloadOptions;
import org.eclipse.kura.core.deployment.install.impl.DeploymentInstallOptionsImpl;
import org.eclipse.kura.message.KuraPayload;
import org.eclipse.kura.message.KuraRequestPayload;

public class DeploymentDownloadOptionsImpl extends DeploymentInstallOptionsImpl implements DeploymentDownloadOptions {

    private String deployUri;
    private String downloadProtocol;
    private int    blockSize;
    private int    notifyBlockSize;
    private int    blockDelay = 0;
    private int    timeout    = 4000;

    private String  username      = null;
    private String  password      = null;
    private boolean forceDownload = false;

    private String hash;

    public DeploymentDownloadOptionsImpl(String deployUri, String dpName, String dpVersion) {
        super(dpName, dpVersion);
        setDeployUri(deployUri);
    }

    public DeploymentDownloadOptionsImpl(KuraPayload request) throws KuraException {
        super(null, null);
        setDeployUri((String) request.getMetric(METRIC_DP_DOWNLOAD_URI));
        if (getDeployUri() == null) {
            throw new KuraInvalidMessageException("Missing deployment package URL!");
        }

        super.setDpName((String) request.getMetric(METRIC_DP_NAME));
        if (super.getDpName() == null) {
            throw new KuraInvalidMessageException("Missing deployment package name!");
        }

        super.setDpVersion((String) request.getMetric(METRIC_DP_VERSION));
        if (super.getDpVersion() == null) {
            throw new KuraInvalidMessageException("Missing deployment package version!");
        }

        setDownloadProtocol((String) request.getMetric(METRIC_DP_DOWNLOAD_PROTOCOL));
        if (getDownloadProtocol() == null) {
            throw new KuraInvalidMessageException("Missing download protocol!");
        }

        super.setJobId((Long) request.getMetric(METRIC_JOB_ID));
        if (super.getJobId() == null) {
            throw new KuraInvalidMessageException("Missing jobId!");
        }

        super.setSystemUpdate((Boolean) request.getMetric(METRIC_DP_INSTALL_SYSTEM_UPDATE));
        if (super.getSystemUpdate() == null) {
            throw new KuraInvalidMessageException("Missing SystemUpdate!");
        }

        try {
            Object metric = request.getMetric(METRIC_DP_DOWNLOAD_BLOCK_SIZE);
            if (metric != null) {
                blockSize = (Integer) metric;
            }
            metric = request.getMetric(METRIC_DP_DOWNLOAD_BLOCK_DELAY);
            if (metric != null) {
                blockDelay = (Integer) metric;
            }
            metric = request.getMetric(METRIC_DP_DOWNLOAD_TIMEOUT);
            if (metric != null) {
                timeout = (Integer) metric;
            }
            metric = request.getMetric(METRIC_DP_DOWNLOAD_RESUME);
            if (metric != null) {
                super.setResume((Boolean) metric);
            }
            metric = request.getMetric(METRIC_DP_DOWNLOAD_USER);
            if (metric != null) {
                username = (String) metric;
            }
            metric = request.getMetric(METRIC_DP_DOWNLOAD_PASSWORD);
            if (metric != null) {
                password = (String) metric;
            }
            metric = request.getMetric(METRIC_DP_DOWNLOAD_HASH);
            if (metric != null) {
                hash = (String) metric;
            }
            metric = request.getMetric(METRIC_DP_INSTALL);
            if (metric != null) {
                super.setInstall((Boolean) metric);
            }
            metric = request.getMetric(METRIC_DP_REBOOT);
            if (metric != null) {
                super.setReboot((Boolean) metric);
            }
            metric = request.getMetric(METRIC_DP_REBOOT_DELAY);
            if (metric != null) {
                super.setRebootDelay((Integer) metric);
            }
            metric = request.getMetric(METRIC_DP_DOWNLOAD_FORCE_DOWNLOAD);
            if (metric != null) {
                forceDownload = (Boolean) metric;
            }

            metric = request.getMetric(METRIC_DP_DOWNLOAD_NOTIFY_BLOCK_SIZE);
            if (metric != null) {
                notifyBlockSize = (Integer) metric;
            }

            metric = request.getMetric(KuraRequestPayload.REQUESTER_CLIENT_ID);
            if (metric != null) {
                super.setRequestClientId((String) metric);
            }

            metric = request.getMetric(METRIC_INSTALL_VERIFIER_URI);
            if (metric != null) {
                super.setVerifierURI((String) metric);
            }

        } catch (Exception ex) {
            throw new KuraException(KuraErrorCode.INTERNAL_ERROR, ex);
        }
    }

    @Override
    public int getNotifyBlockSize() {
        return notifyBlockSize;
    }

    @Override
    public void setNotifyBlockSize(int notifyBlockSize) {
        this.notifyBlockSize = notifyBlockSize;
    }

    @Override
    public String getDeployUri() {
        return deployUri;
    }

    @Override
    public void setDeployUri(String deployUri) {
        this.deployUri = deployUri;
    }

    @Override
    public String getDownloadProtocol() {
        return downloadProtocol;
    }

    @Override
    public void setDownloadProtocol(String downloadProtocol) {
        this.downloadProtocol = downloadProtocol;
    }

    @Override
    public int getBlockSize() {
        return blockSize;
    }

    @Override
    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    @Override
    public int getBlockDelay() {
        return blockDelay;
    }

    @Override
    public void setBlockDelay(int blockDelay) {
        this.blockDelay = blockDelay;
    }

    @Override
    public int getTimeout() {
        return timeout;
    }

    @Override
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public boolean isDownloadForced() {
        return forceDownload;
    }

    @Override
    public void setDownloadForced(boolean forceDownload) {
        this.forceDownload = forceDownload;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }
    
    @Override
    public String getHash() {
        return hash;
    }

    @Override
    public void setHash(String hash) {
        this.hash = hash;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + blockDelay;
        result = prime * result + blockSize;
        result = prime * result + ((deployUri == null) ? 0 : deployUri.hashCode());
        result = prime * result + ((downloadProtocol == null) ? 0 : downloadProtocol.hashCode());
        result = prime * result + (forceDownload ? 1231 : 1237);
        result = prime * result + ((hash == null) ? 0 : hash.hashCode());
        result = prime * result + notifyBlockSize;
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + timeout;
        result = prime * result + ((username == null) ? 0 : username.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DeploymentDownloadOptionsImpl)) {
            return false;
        }
        DeploymentDownloadOptionsImpl other = (DeploymentDownloadOptionsImpl) obj;
        if (blockDelay != other.blockDelay) {
            return false;
        }
        if (blockSize != other.blockSize) {
            return false;
        }
        if (deployUri == null) {
            if (other.deployUri != null) {
                return false;
            }
        } else if (!deployUri.equals(other.deployUri)) {
            return false;
        }
        if (downloadProtocol == null) {
            if (other.downloadProtocol != null) {
                return false;
            }
        } else if (!downloadProtocol.equals(other.downloadProtocol)) {
            return false;
        }
        if (forceDownload != other.forceDownload) {
            return false;
        }
        if (hash == null) {
            if (other.hash != null) {
                return false;
            }
        } else if (!hash.equals(other.hash)) {
            return false;
        }
        if (notifyBlockSize != other.notifyBlockSize) {
            return false;
        }
        if (password == null) {
            if (other.password != null) {
                return false;
            }
        } else if (!password.equals(other.password)) {
            return false;
        }
        if (timeout != other.timeout) {
            return false;
        }
        if (username == null) {
            if (other.username != null) {
                return false;
            }
        } else if (!username.equals(other.username)) {
            return false;
        }
        return true;
    }
}
