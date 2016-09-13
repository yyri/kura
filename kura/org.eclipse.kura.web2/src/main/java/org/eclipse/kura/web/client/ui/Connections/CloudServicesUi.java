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
package org.eclipse.kura.web.client.ui.Connections;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtFirewallOpenPortEntry;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudService;
import org.eclipse.kura.web.shared.service.GwtCloudServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;

public class CloudServicesUi extends Composite {
    private static final Logger   logger = Logger.getLogger(CloudServicesUi.class.getSimpleName());
    private static final Messages MSG    = GWT.create(Messages.class);

    private static CloudServicesUiUiBinder uiBinder = GWT.create(CloudServicesUiUiBinder.class);

    private final SingleSelectionModel<GwtCloudConnectionEntry> selectionModel  = new SingleSelectionModel<GwtCloudConnectionEntry>();
    private final GwtCloudServiceAsync                          gwtCloudService = GWT.create(GwtCloudService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    
    private ListDataProvider<GwtCloudConnectionEntry> cloudServicesDataProvider = new ListDataProvider<GwtCloudConnectionEntry>();

    interface CloudServicesUiUiBinder extends UiBinder<Widget, CloudServicesUi> {}

    private Map<String, Button> connectButtons    = new HashMap<String, Button>();
    private Map<String, Button> disconnectButtons = new HashMap<String, Button>();

    @UiField
    Well      connectionsWell;
    @UiField
    Well      connectionEditWell;
    @UiField
    Button    newConnection;
    @UiField
    Button    deleteConnection;
    @UiField
    Button    statusConnect;
    @UiField
    Button    statusDisconnect;
    @UiField
    Button    cancel;
    @UiField
    Button    btnCreateComp;
    @UiField
    PanelBody connectPanel;
    @UiField
    Modal     connectModal;
    @UiField
    Modal     newConnectionModal;
    @UiField
    ListBox   cloudFactoriesPids;
    @UiField
    TextBox   cloudServicePid;
    

    @UiField
    CellTable<GwtCloudConnectionEntry> connectionsGrid    = new CellTable<GwtCloudConnectionEntry>();
    @UiField
    CellTable<GwtCloudConnectionEntry> connectionEditGrid = new CellTable<GwtCloudConnectionEntry>();

    public CloudServicesUi() {
        logger.log(Level.FINER, "Initializing StatusPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));

        // Set text for buttons
        newConnection.setText(MSG.newButton());
        deleteConnection.setText(MSG.deleteButton());
        statusConnect.setText(MSG.connectButton());
        statusDisconnect.setText(MSG.disconnectButton());
        cancel.setText(MSG.cancelButton());
        connectionsGrid.setSelectionModel(selectionModel);

        newConnection.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showNewConnectionModal();
            }
        });
        
        statusConnect.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showConnectModal();
            }
        });

        statusDisconnect.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                showConnectModal();
            }
        });

        cancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hideConnectModal();
            }
        });
        
        btnCreateComp.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                createComponent();
            }
        });

        initConnectionsTable();
    }

    public boolean isDirty() {
        return false;
    }

    public void refresh() {
        EntryClassUi.showWaitModal();
        cloudServicesDataProvider.getList().clear();

        gwtCloudService.findCloudServices(new AsyncCallback<List<GwtCloudConnectionEntry>>() {
            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught, gwtCloudService.getClass().getSimpleName());
            }

            @Override
            public void onSuccess(List<GwtCloudConnectionEntry> result) {
                for (GwtCloudConnectionEntry pair : result) {
                    cloudServicesDataProvider.getList().add(pair);
                }
                refreshTable();
                setVisibility();
                EntryClassUi.hideWaitModal();
            }
        });
    }
    
    private void refreshTable() {
        int size = cloudServicesDataProvider.getList().size();
        connectionsGrid.setVisibleRange(0, size);
        cloudServicesDataProvider.flush();
        connectionsGrid.redraw();
    }
    
    private void setVisibility() {
        if (cloudServicesDataProvider.getList().isEmpty()) {
            connectionsGrid.setVisible(false);
//            notification.setVisible(true);
//            notification.setText(MSGS.firewallOpenPortTableNoPorts());
        } else {
            connectionsGrid.setVisible(true);
//            notification.setVisible(false);
        }
    }

    
    //
    // Private methods
    //
    private void showNewConnectionModal() {
        EntryClassUi.showWaitModal();
        cloudFactoriesPids.clear();

        gwtCloudService.findCloudServiceFactories(new AsyncCallback<List<GwtGroupedNVPair>>() {
            @Override
            public void onFailure(Throwable caught) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(caught, gwtCloudService.getClass().getSimpleName());
            }

            @Override
            public void onSuccess(List<GwtGroupedNVPair> result) {
                for (GwtGroupedNVPair pair : result) {
                    cloudFactoriesPids.addItem(pair.getValue());
                }
                EntryClassUi.hideWaitModal();
            }
        });
        
        newConnectionModal.show();
    }
    
    private void showConnectModal() {
        connectModal.show();
    }

    private void hideConnectModal() {
        connectModal.hide();
    }

    private void initConnectionsTable() {

        TextColumn<GwtCloudConnectionEntry> col1 = new TextColumn<GwtCloudConnectionEntry>() {
            @Override
            public String getValue(GwtCloudConnectionEntry object) {
                if (object.isConnected()) {
                    return "Y";
                } else {
                    return "N";
                }
            }
        };
        col1.setCellStyleNames("status-table-row");
        connectionsGrid.addColumn(col1, "Connected?");

        TextColumn<GwtCloudConnectionEntry> col2 = new TextColumn<GwtCloudConnectionEntry>() {
            @Override
            public String getValue(GwtCloudConnectionEntry object) {
                if (object.getCloudFactoryPid() != null) {
                    return String.valueOf(object.getCloudFactoryPid());
                } else {
                    return "";
                }
            }
        };
        col2.setCellStyleNames("status-table-row");
        connectionsGrid.addColumn(col2, "Cloud Factory Pid"); // MSGS.firewallOpenPortProtocol()

        TextColumn<GwtCloudConnectionEntry> col3 = new TextColumn<GwtCloudConnectionEntry>() {
            @Override
            public String getValue(GwtCloudConnectionEntry object) {
                if (object.getCloudServicePid() != null) {
                    return String.valueOf(object.getCloudServicePid());
                } else {
                    return "";
                }
            }
        };
        col3.setCellStyleNames("status-table-row");
        connectionsGrid.addColumn(col3, "Cloud Service Pid");

        cloudServicesDataProvider.addDataDisplay(connectionsGrid);
    }
    
    private void createComponent() {
        final String factoryPid= cloudFactoriesPids.getSelectedValue();
        final String newCloudServicePid = cloudServicePid.getValue();
        
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken> () {
            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtCloudService.createCloudServiceFromFactory(token, factoryPid, newCloudServicePid, new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught, gwtCloudService.getClass().getSimpleName());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        newConnectionModal.hide();
                        EntryClassUi.hideWaitModal();
                    }
                });
            }

        });
        
    }

}
