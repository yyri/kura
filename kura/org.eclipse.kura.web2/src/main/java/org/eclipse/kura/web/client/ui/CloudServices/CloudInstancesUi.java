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

import java.util.List;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudService;
import org.eclipse.kura.web.shared.service.GwtCloudServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.eclipse.kura.web.shared.service.GwtStatusServiceAsync;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.Well;
import org.gwtbootstrap3.client.ui.gwt.CellTable;
import org.gwtbootstrap3.client.ui.html.Span;

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
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class CloudInstancesUi extends Composite {

    private static CloudConnectionsUiUiBinder uiBinder = GWT.create(CloudConnectionsUiUiBinder.class);
    private static final Messages MSG = GWT.create(Messages.class);

    private final SingleSelectionModel<GwtCloudConnectionEntry> selectionModel = new SingleSelectionModel<GwtCloudConnectionEntry>();
    private final ListDataProvider<GwtCloudConnectionEntry> cloudServicesDataProvider = new ListDataProvider<GwtCloudConnectionEntry>();
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtCloudServiceAsync gwtCloudService = GWT.create(GwtCloudService.class);
    private final GwtStatusServiceAsync gwtStatusService = GWT.create(GwtStatusService.class);

    private final CloudServicesUi cloudServicesUi;

    interface CloudConnectionsUiUiBinder extends UiBinder<Widget, CloudInstancesUi> {
    }

    @UiField
    Well connectionsWell;
    @UiField
    Button connectionRefresh;
    @UiField
    Button newConnection;
    @UiField
    Button deleteConnection;
    @UiField
    Button statusConnect;
    @UiField
    Button statusDisconnect;
    @UiField
    Button btnCreateComp;
    @UiField
    Modal newConnectionModal;
    @UiField
    ListBox cloudFactoriesPids;
    @UiField
    TextBox cloudServicePid;
    @UiField
    CellTable<GwtCloudConnectionEntry> connectionsGrid = new CellTable<GwtCloudConnectionEntry>();

    public CloudInstancesUi(final CloudServicesUi cloudServicesUi) {
        initWidget(uiBinder.createAndBindUi(this));
        this.cloudServicesUi = cloudServicesUi;

        // Set text for buttons
        connectionRefresh.setText(MSG.refresh());
        newConnection.setText(MSG.newButton());
        deleteConnection.setText(MSG.deleteButton());
        statusConnect.setText(MSG.connectButton());
        statusDisconnect.setText(MSG.disconnectButton());
        connectionsGrid.setSelectionModel(selectionModel);

        btnCreateComp.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (cloudServicePid.validate() && !cloudServicePid.getText().trim().isEmpty()) {
                    createComponent();
                }
            }
        });

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {

            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                cloudServicesUi.onSelectionChange();
            }
        });

        cloudServicePid.setValidateOnBlur(true);
        cloudServicePid.setAllowBlank(false);

        initConnectionButtons();

        initConnectionsTable();
    }

    public void loadData() {
        cloudServicesDataProvider.getList().clear();

        gwtCloudService.findCloudServices(new AsyncCallback<List<GwtCloudConnectionEntry>>() {

            @Override
            public void onFailure(Throwable caught) {
                FailureHandler.handle(caught, gwtCloudService.getClass().getSimpleName());
            }

            @Override
            public void onSuccess(List<GwtCloudConnectionEntry> result) {
                for (GwtCloudConnectionEntry pair : result) {
                    cloudServicesDataProvider.getList().add(pair);
                }
                cloudServicesUi.refreshInternal();
            }
        });
    }

    public int getTableSize() {
        return cloudServicesDataProvider.getList().size();
    }

    public void setVisibility(boolean isVisible) {
        connectionsGrid.setVisible(isVisible);
    }

    public GwtCloudConnectionEntry getSelectedObject() {
        return selectionModel.getSelectedObject();
    }

    public void setSelected(GwtCloudConnectionEntry cloudEntry) {
        selectionModel.setSelected(cloudEntry, true);
    }

    private void initConnectionButtons() {
        connectionRefresh.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                cloudServicesUi.refresh();
            }
        });

        newConnection.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                showNewConnectionModal();
            }
        });

        deleteConnection.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (getTableSize() > 0) {
                    showDeleteModal();
                }
            }
        });

        statusConnect.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtCloudConnectionEntry selection = selectionModel.getSelectedObject();
                final String selectedCloudServicePid = selection.getCloudServicePid();
                connectDataService(selectedCloudServicePid);
            }
        });

        statusDisconnect.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtCloudConnectionEntry selection = selectionModel.getSelectedObject();
                final String selectedCloudServicePid = selection.getCloudServicePid();
                disconnectDataService(selectedCloudServicePid);
            }
        });
    }

    private void initConnectionsTable() {

        TextColumn<GwtCloudConnectionEntry> col1 = new TextColumn<GwtCloudConnectionEntry>() {

            @Override
            public String getValue(GwtCloudConnectionEntry object) {
                if (object.isConnected()) {
                    return MSG.yesButton();
                } else {
                    return MSG.noButton();
                }
            }
        };
        col1.setCellStyleNames("status-table-row");
        connectionsGrid.addColumn(col1, MSG.connectedLabel());

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
        connectionsGrid.addColumn(col2, MSG.connectionCloudFactoryLabel());

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
        connectionsGrid.addColumn(col3, MSG.connectionCloudServiceLabel());

        cloudServicesDataProvider.addDataDisplay(connectionsGrid);
    }

    private void createComponent() {
        final String factoryPid = cloudFactoriesPids.getSelectedValue();
        final String newCloudServicePid = cloudServicePid.getValue();

        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

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
                        cloudServicesUi.refresh(2000);
                    }
                });
            }

        });
    }

    public void refresh() {
        int size = cloudServicesDataProvider.getList().size();
        connectionsGrid.setVisibleRange(0, size);
        cloudServicesDataProvider.flush();

        if (size > 0) {
            GwtCloudConnectionEntry firstEntry = cloudServicesDataProvider.getList().get(0);
            selectionModel.setSelected(firstEntry, true);
        }
        connectionsGrid.redraw();
    }

    private void showNewConnectionModal() {
        EntryClassUi.showWaitModal();
        cloudServicePid.clear();
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

    private void connectDataService(final String connectionId) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtStatusService.connectDataService(token, connectionId, new AsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                        cloudServicesUi.refresh(1000);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }
                });
            }
        });
    }

    private void disconnectDataService(final String connectionId) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtStatusService.disconnectDataService(token, connectionId, new AsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                        cloudServicesUi.refresh(1000);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }
                });
            }
        });
    }

    private void deleteConnection(final String factoryPid, final String cloudServicePid) {
        EntryClassUi.showWaitModal();
        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                EntryClassUi.hideWaitModal();
                FailureHandler.handle(ex);
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtCloudService.deleteCloudServiceFromFactory(token, factoryPid, cloudServicePid, new AsyncCallback<Void>() {

                    @Override
                    public void onSuccess(Void result) {
                        EntryClassUi.hideWaitModal();
                        cloudServicesUi.refresh(2000);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        EntryClassUi.hideWaitModal();
                        FailureHandler.handle(caught);
                    }
                });
            }
        });
    }

    private void showDeleteModal() {
        final Modal modal = new Modal();

        ModalHeader header = new ModalHeader();
        header.setTitle(MSG.warning());
        modal.add(header);

        ModalBody body = new ModalBody();
        body.add(new Span(MSG.cloudServiceDeleteConfirmation()));
        modal.add(body);

        ModalFooter footer = new ModalFooter();
        footer.add(new Button(MSG.yesButton(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                GwtCloudConnectionEntry selection = selectionModel.getSelectedObject();
                final String selectedFactoryPid = selection.getCloudFactoryPid();
                final String selectedCloudServicePid = selection.getCloudServicePid();
                deleteConnection(selectedFactoryPid, selectedCloudServicePid);
                modal.hide();
            }
        }));
        footer.add(new Button(MSG.noButton(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                modal.hide();
            }
        }));
        modal.add(footer);
        modal.show();
    }

}
