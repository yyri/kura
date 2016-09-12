/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and/or its affiliates
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *     Jens Reimann <jreimann@redhat.com> - Fix logging calls
 *         - Fix possible NPE
 *******************************************************************************/
package org.eclipse.kura.web.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.kura.cloud.CloudService;
import org.eclipse.kura.web.server.util.ServiceLocator;
import org.eclipse.kura.web.shared.GwtKuraException;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.service.GwtCloudService;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GwtCloudServiceImpl extends OsgiRemoteServiceServlet implements GwtCloudService {
    /**
     * 
     */
    private static final long serialVersionUID = 2595835826149606703L;
    private static final Logger s_logger = LoggerFactory.getLogger(GwtCloudServiceImpl.class);

    @Override
    public List<GwtCloudConnectionEntry> findCloudServices() throws GwtKuraException {
        List<GwtCloudConnectionEntry> pairs = new ArrayList<GwtCloudConnectionEntry>();
        Collection<ServiceReference<CloudService>> cloudServiceReferences = ServiceLocator.getInstance().getServiceReferences(CloudService.class, null);
        
        for (ServiceReference<CloudService> cloudServiceReference : cloudServiceReferences) {
            String cloudServicePid = (String) cloudServiceReference.getProperty("kura.service.pid");
            CloudService cloudService= ServiceLocator.getInstance().getService(cloudServiceReference);
            
            GwtCloudConnectionEntry cloudConnectionEntry = new GwtCloudConnectionEntry();
            cloudConnectionEntry.setConnectionStatus(cloudService.isConnected());
            cloudConnectionEntry.setCloudFactoryPid(cloudServicePid);
            cloudConnectionEntry.setCloudServicePid(cloudServicePid);
            pairs.add(cloudConnectionEntry);
            ServiceLocator.getInstance().ungetService(cloudServiceReference);
        }
        return pairs;
    }

}
