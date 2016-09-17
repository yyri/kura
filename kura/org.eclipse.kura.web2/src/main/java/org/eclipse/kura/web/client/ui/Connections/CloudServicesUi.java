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

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.kura.web.client.messages.Messages;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtCloudConnectionEntry;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtGroupedNVPair;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtCloudService;
import org.eclipse.kura.web.shared.service.GwtCloudServiceAsync;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.eclipse.kura.web.shared.service.GwtStatusService;
import org.eclipse.kura.web.shared.service.GwtStatusServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ListBox;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavTabs;
import org.gwtbootstrap3.client.ui.TabContent;
import org.gwtbootstrap3.client.ui.TabListItem;
import org.gwtbootstrap3.client.ui.TabPane;
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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

public class CloudServicesUi extends Composite {

    private static final Logger logger = Logger.getLogger(CloudServicesUi.class.getSimpleName());
    private static final Messages MSG = GWT.create(Messages.class);
    private static final String KURA_CLOUD_SERVICE_FACTORY_PID = "kura.cloud.service.factory.pid";

    private static CloudServicesUiUiBinder uiBinder = GWT.create(CloudServicesUiUiBinder.class);

    private final SingleSelectionModel<GwtCloudConnectionEntry> selectionModel = new SingleSelectionModel<GwtCloudConnectionEntry>();
    private final GwtCloudServiceAsync gwtCloudService = GWT.create(GwtCloudService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtStatusServiceAsync gwtStatusService = GWT.create(GwtStatusService.class);

    private final ListDataProvider<GwtCloudConnectionEntry> cloudServicesDataProvider = new ListDataProvider<GwtCloudConnectionEntry>();

    interface CloudServicesUiUiBinder extends UiBinder<Widget, CloudServicesUi> {
    }

    private boolean dirty;

    @UiField
    Well connectionsWell;
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
    TabContent connectionTabContent;
    @UiField
    NavTabs connectionNavtabs;
    @UiField
    Alert notification;

    @UiField
    CellTable<GwtCloudConnectionEntry> connectionsGrid = new CellTable<GwtCloudConnectionEntry>();

    public CloudServicesUi() {
        logger.log(Level.FINER, "Initializing StatusPanelUi...");
        initWidget(uiBinder.createAndBindUi(this));

        // Set text for buttons
        newConnection.setText(MSG.newButton());
        deleteConnection.setText(MSG.deleteButton());
        statusConnect.setText(MSG.connectButton());
        statusDisconnect.setText(MSG.disconnectButton());
        connectionsGrid.setSelectionModel(selectionModel);

        cloudServicePid.setValidateOnBlur(true);
        cloudServicePid.setAllowBlank(false);

        newConnection.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                showNewConnectionModal();
            }
        });

        deleteConnection.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                showDeleteModal();
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
                if (!isDirty()) {
                    selectConnection();
                } else {
                    showDirtyModal();
                }
            }
        });

        initConnectionsTable();
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

    private void refresh(int delay) {
        Timer timer = new Timer() {

            @Override
            public void run() {
                refresh();
            }
        };
        timer.schedule(delay);
    }

    private void refreshTable() {
        int size = cloudServicesDataProvider.getList().size();
        connectionsGrid.setVisibleRange(0, size);
        cloudServicesDataProvider.flush();

        if (size > 0) {
            GwtCloudConnectionEntry firstEntry = cloudServicesDataProvider.getList().get(0);
            selectionModel.setSelected(firstEntry, true);
        }
        connectionsGrid.redraw();
    }

    private void setVisibility() {
        if (cloudServicesDataProvider.getList().isEmpty()) {
            connectionsGrid.setVisible(false);
            connectionNavtabs.setVisible(false);
            connectionTabContent.setVisible(false);
            notification.setVisible(true);
            notification.setText(MSG.noConnectionsAvailable());
        } else {
            connectionsGrid.setVisible(true);
            connectionNavtabs.setVisible(true);
            connectionTabContent.setVisible(true);
            notification.setVisible(false);
        }
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        for (int connectionTabIndex = 0; connectionTabIndex < connectionTabContent
                .getWidgetCount(); connectionTabIndex++) {
            TabPane pane = (TabPane) connectionTabContent.getWidget(connectionTabIndex);
            for (int paneIndex = 0; paneIndex < pane.getWidgetCount(); paneIndex++) {
                ServiceConfigurationUi serviceConfigUi = (ServiceConfigurationUi) pane.getWidget(paneIndex);
                serviceConfigUi.setDirty(dirty);
            }
        }
    }

    public boolean isDirty() {
        for (int connectionTabIndex = 0; connectionTabIndex < connectionTabContent
                .getWidgetCount(); connectionTabIndex++) {
            TabPane pane = (TabPane) connectionTabContent.getWidget(connectionTabIndex);
            for (int paneIndex = 0; paneIndex < pane.getWidgetCount(); paneIndex++) {
                ServiceConfigurationUi serviceConfigUi = (ServiceConfigurationUi) pane.getWidget(paneIndex);
                dirty = dirty || serviceConfigUi.isDirty();
            }
        }
        return dirty;
    }

    //
    // Private methods
    //
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
                        refresh(2000);
                    }
                });
            }

        });
    }

    private void selectConnection() {
        connectionNavtabs.clear();
        connectionTabContent.clear();

        GwtCloudConnectionEntry selection = selectionModel.getSelectedObject();
        final String selectedCloudServicePid = selection.getCloudServicePid();

        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

            @Override
            public void onFailure(Throwable ex) {
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(GwtXSRFToken token) {
                gwtComponentService.findComponentConfiguration(token, selectedCloudServicePid, new AsyncCallback<List<GwtConfigComponent>>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        logger.log(Level.SEVERE, ex.getMessage(), ex);
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(List<GwtConfigComponent> result) {
                        for (GwtConfigComponent pair : result) {
                            if (selectedCloudServicePid.equals(pair.getComponentId()) && pair.getParameter(KURA_CLOUD_SERVICE_FACTORY_PID) != null) {
                                String factoryPid = pair.getParameter(KURA_CLOUD_SERVICE_FACTORY_PID).getValue();
                                getCloudStackConfigurations(factoryPid, selectedCloudServicePid);
                            }
                        }
                    }
                });
            }
        });
    }

    private void getCloudStackConfigurations(String factoryPid, String cloudServicePid) {
        gwtCloudService.findStackPidsByFactory(factoryPid, cloudServicePid, new AsyncCallback<List<String>>() {

            @Override
            public void onFailure(Throwable ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
                FailureHandler.handle(ex, EntryClassUi.class.getName());
            }

            @Override
            public void onSuccess(List<String> result) {
                final List<String> pidsResult = result;

                gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                    @Override
                    public void onFailure(Throwable ex) {
                        FailureHandler.handle(ex, EntryClassUi.class.getName());
                    }

                    @Override
                    public void onSuccess(GwtXSRFToken token) {
                        gwtComponentService.findFilteredComponentConfigurations(token, new AsyncCallback<List<GwtConfigComponent>>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                logger.log(Level.SEVERE, ex.getMessage(), ex);
                                FailureHandler.handle(ex, EntryClassUi.class.getName());
                            }

                            @Override
                            public void onSuccess(List<GwtConfigComponent> result) {
                                boolean isFirstEntry = true;
                                for (GwtConfigComponent pair : result) {
                                    if (pidsResult.contains(pair.getComponentId())) {
                                        renderTabs(pair, isFirstEntry);
                                        isFirstEntry = false;
                                    }
                                }
                            }
                        });
                    }
                });
            }
        });
    }

    private void renderTabs(GwtConfigComponent config, boolean isFirstEntry) {
        String selectedCloudServicePid = config.getComponentId();
        String name0;
        int start = selectedCloudServicePid.lastIndexOf('.');
        int substringIndex = start + 1;
        if (start != -1 && substringIndex < selectedCloudServicePid.length()) {
            name0 = selectedCloudServicePid.substring(substringIndex);
        } else {
            name0 = selectedCloudServicePid;
        }
        final String simplifiedComponentName = name0;
        TabListItem item = new TabListItem(simplifiedComponentName);
        item.setActive(isFirstEntry);
        item.setDataTarget("#" + simplifiedComponentName);

        connectionNavtabs.add(item);

        TabPane tabPane = new TabPane();
        tabPane.setId(simplifiedComponentName);
        tabPane.setActive(isFirstEntry);
        ServiceConfigurationUi serviceConfigurationBinder = new ServiceConfigurationUi(config);
        tabPane.add(serviceConfigurationBinder);

        connectionTabContent.add(tabPane);

        serviceConfigurationBinder.renderForm();
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
                        refresh(1000);
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
                        refresh(1000);
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
                        refresh(2000);
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

    private void showDirtyModal() {
        final Modal modal = new Modal();

        ModalHeader header = new ModalHeader();
        header.setTitle(MSG.warning());
        modal.add(header);

        ModalBody body = new ModalBody();
        body.add(new Span(MSG.deviceConfigDirty()));
        modal.add(body);

        ModalFooter footer = new ModalFooter();
        footer.add(new Button(MSG.okButton(), new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                modal.hide();
            }
        }));
        modal.add(footer);
        modal.show();
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
