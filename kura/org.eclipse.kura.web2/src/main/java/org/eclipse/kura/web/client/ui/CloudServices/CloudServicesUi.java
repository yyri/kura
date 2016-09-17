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
 *******************************************************************************/
package org.eclipse.kura.web.client.ui.CloudServices;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.Panel;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.html.Span;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class CloudServicesUi extends Composite {

    private static final Logger logger = Logger.getLogger(CloudServicesUi.class.getSimpleName());
    private static final Messages MSG = GWT.create(Messages.class);

    private static CloudServicesUiUiBinder uiBinder = GWT.create(CloudServicesUiUiBinder.class);
    private static CloudInstancesUi cloudInstancesBinder;
    private static CloudServiceConfigurationsUi cloudServiceConfigurationsBinder;

    private GwtCloudConnectionEntry currentlySelectedEntry;
    private TabListItem currentlySelectedTab;

    interface CloudServicesUiUiBinder extends UiBinder<Widget, CloudServicesUi> {
    }

    @UiField
    Panel cloudInstancesPanel;
    @UiField
    Panel cloudConfigurationsPanel;
    @UiField
    Alert notification;

    public CloudServicesUi() {
        logger.log(Level.FINER, "Initializing StatusPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));

        cloudInstancesBinder = new CloudInstancesUi(this);
        cloudInstancesPanel.add(cloudInstancesBinder);

        cloudServiceConfigurationsBinder = new CloudServiceConfigurationsUi(this);
        cloudConfigurationsPanel.add(cloudServiceConfigurationsBinder);
    }

    public void refresh() {
        cloudInstancesBinder.loadData();
        // setVisibility();
    }

    protected void refreshInternal() {
        cloudInstancesBinder.refresh();
        currentlySelectedEntry = cloudInstancesBinder.getSelectedObject();
        currentlySelectedTab = cloudServiceConfigurationsBinder.getSelectedTab();
        setVisibility();
    }

    public void setDirty(boolean dirty) {
        cloudServiceConfigurationsBinder.setDirty(dirty);
    }

    public boolean isDirty() {
        return cloudServiceConfigurationsBinder.isDirty();
    }

    //
    // Private methods
    //
    private void setVisibility() {
        if (cloudInstancesBinder.getTableSize() == 0) {
            cloudInstancesBinder.setVisibility(false);
            cloudServiceConfigurationsBinder.setVisibility(false);
            cloudConfigurationsPanel.setVisible(false);
            notification.setVisible(true);
            notification.setText(MSG.noConnectionsAvailable());
        } else {
            cloudInstancesBinder.setVisibility(true);
            cloudServiceConfigurationsBinder.setVisibility(true);
            cloudConfigurationsPanel.setVisible(true);
            notification.setVisible(false);
        }
    }

    protected void onSelectionChange() {
        GwtCloudConnectionEntry selectedInstanceEntry = cloudInstancesBinder.getSelectedObject();

        if (!isDirty()) {
            if (selectedInstanceEntry != null) {
                currentlySelectedEntry = selectedInstanceEntry;
                cloudServiceConfigurationsBinder.selectConnection(selectedInstanceEntry);
            }
        } else {
            if (selectedInstanceEntry != currentlySelectedEntry) {
                showDirtyModal();
            }
        }
    }

    protected void onTabSelectionChange(TabListItem newTab) {
        currentlySelectedTab = cloudServiceConfigurationsBinder.getSelectedTab();
        if (isDirty() && newTab != currentlySelectedTab) {
            showDirtyModal();
        } else {
            currentlySelectedTab = newTab;
            cloudServiceConfigurationsBinder.setSelectedTab(currentlySelectedTab);
        }
    }

    private void showDirtyModal() {
        final Modal modal = new Modal();

        ModalHeader header = new ModalHeader();
        header.setTitle(MSG.confirm());
        modal.add(header);

        ModalBody body = new ModalBody();
        body.add(new Span(MSG.deviceConfigDirty()));
        modal.add(body);

        ModalFooter footer = new ModalFooter();
        ButtonGroup group = new ButtonGroup();
        Button yes = new Button();
        yes.setText(MSG.yesButton());
        yes.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                modal.hide();
                GwtCloudConnectionEntry selectedInstanceEntry = cloudInstancesBinder.getSelectedObject();
                if (selectedInstanceEntry != null) {
                    currentlySelectedEntry = selectedInstanceEntry;
                    cloudServiceConfigurationsBinder.selectConnection(selectedInstanceEntry);
                }

                CloudServiceConfigurationUi dirtyConfig = cloudServiceConfigurationsBinder.getDirtyCloudConfiguration();
                if (dirtyConfig != null) {
                    dirtyConfig.resetVisualization();
                }
                
                setDirty(false);
            }
        });
        group.add(yes);
        Button no = new Button();
        no.setText(MSG.noButton());
        no.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                cloudInstancesBinder.setSelected(currentlySelectedEntry);
                currentlySelectedTab.showTab();
                modal.hide();
            }
        });
        group.add(no);
        footer.add(group);
        modal.add(footer);
        modal.show();
    }

    protected void refresh(int delay) {
        Timer timer = new Timer() {

            @Override
            public void run() {
                refresh();
            }
        };
        timer.schedule(delay);
    }

}
