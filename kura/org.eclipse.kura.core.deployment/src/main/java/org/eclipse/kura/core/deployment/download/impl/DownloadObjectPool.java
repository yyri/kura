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

import org.eclipse.kura.core.deployment.download.DeploymentDownloadOptions;
import org.eclipse.kura.core.deployment.download.DownloadOps;

public class DownloadObjectPool { // Fake pool: only one download allowed for
 // the time being

    private static DownloadOps downloadInstance;

    private DownloadObjectPool() {
    }

    public static DownloadOps getInstance() {
        if (downloadInstance == null) {
            downloadInstance = new DownloadOpsImpl();
        }
        return downloadInstance;
    }

    public static DownloadOps getInstance(DeploymentDownloadOptions options) {
        if (downloadInstance == null) {
            downloadInstance = new DownloadOpsImpl();
            return downloadInstance;
        } else if (downloadInstance.getDownloadOptions().equals(options)) {
            return downloadInstance;
        }
        return null;
    }

    public static void returnObject(DownloadOps instance) {
        if (instance.equals(downloadInstance)) {
            downloadInstance = null;
        }
    }
}
