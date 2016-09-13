/*******************************************************************************
 * Copyright (c) 2011, 2016 Eurotech and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech
 *******************************************************************************/
/*
 * Render the Content in the Main Panel corressponding to Service (GwtBSConfigComponent) selected in the Services Panel
 * 
 * Fields are rendered based on their type (Password(Input), Choice(Dropboxes) etc. with Text fields rendered
 * for both numeric and other textual field with validate() checking if value in numeric fields is numeric
 */
package org.eclipse.kura.web.client.ui;

import java.util.Iterator;
import java.util.logging.Level;

import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.AnchorListItem;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.NavPills;
import org.gwtbootstrap3.client.ui.PanelBody;
import org.gwtbootstrap3.client.ui.TextBox;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class ServicesUi extends AbstractServicesUi {
    
    private static final ServicesUiUiBinder uiBinder         = GWT.create(ServicesUiUiBinder.class);

    interface ServicesUiUiBinder extends UiBinder<Widget, ServicesUi> {}

    private final GwtComponentServiceAsync     gwtComponentService = GWT.create(GwtComponentService.class);
    private final GwtSecurityTokenServiceAsync gwtXSRFService      = GWT.create(GwtSecurityTokenService.class);

    private boolean    dirty, initialized;

    NavPills       menu;
    PanelBody      content;
    AnchorListItem service;
    TextBox        validated;
    FormGroup      validatedGroup;
    EntryClassUi   entryClass;
    Modal          modal;

    @UiField
    Button   apply, reset;
    @UiField
    FieldSet fields;
    @UiField
    Form     form;

    @UiField
    Modal incompleteFieldsModal;
    @UiField
    Alert incompleteFields;
    @UiField
    Text  incompleteFieldsText;

    //
    // Public methods
    //
    public ServicesUi(final GwtConfigComponent addedItem, EntryClassUi entryClassUi) {
        initWidget(uiBinder.createAndBindUi(this));
        initialized = false;
        entryClass = entryClassUi;
        m_configurableComponent = addedItem;
        fields.clear();
        setOriginalValues(m_configurableComponent);

        apply.setText(MSGS.apply());
        apply.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                apply();
            }
        });

        reset.setText(MSGS.reset());
        reset.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                reset();
            }
        });
        renderForm();
        initInvalidDataModal();

        setDirty(false);
        apply.setEnabled(false);
        reset.setEnabled(false);
    }

    @Override
    public void setDirty(boolean flag) {
        dirty = flag;
        if (dirty && initialized) {
            apply.setEnabled(true);
            reset.setEnabled(true);
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public void reset() {
        if (isDirty()) {
            // Modal
            modal = new Modal();

            ModalHeader header = new ModalHeader();
            header.setTitle(MSGS.confirm());
            modal.add(header);

            ModalBody body = new ModalBody();
            body.add(new Span(MSGS.deviceConfigDirty()));
            modal.add(body);

            ModalFooter footer = new ModalFooter();
            ButtonGroup group = new ButtonGroup();
            Button yes = new Button();
            yes.setText(MSGS.yesButton());
            yes.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    modal.hide();
                    renderForm();
                    apply.setEnabled(false);
                    reset.setEnabled(false);
                    setDirty(false);
                    entryClass.initServicesTree();
                }
            });
            group.add(yes);
            Button no = new Button();
            no.setText(MSGS.noButton());
            no.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    modal.hide();
                }
            });
            group.add(no);
            footer.add(group);
            modal.add(footer);
            modal.show();
        }                // end is dirty
    }

    // TODO: Separate render methods for each type (ex: Boolean, String,
    // Password, etc.). See latest org.eclipse.kura.web code.
    // Iterates through all GwtConfigParameter in the selected
    // GwtConfigComponent
    @Override
    public void renderForm() {
        fields.clear();
        for (GwtConfigParameter param : m_configurableComponent.getParameters()) {
            if (param.getCardinality() == 0 || param.getCardinality() == 1 || param.getCardinality() == -1) {
                FormGroup formGroup = new FormGroup();
                renderConfigParameter(param, true, formGroup);
            } else {
                renderMultiFieldConfigParameter(param);
            }
        }
        initialized = true;
    }
    
    @Override
    protected void renderTextField(final GwtConfigParameter param, boolean isFirstInstance, final FormGroup formGroup) {
        super.renderTextField(param, isFirstInstance, formGroup);
        fields.add(formGroup);
    }
    
    @Override
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderPasswordField(param, isFirstInstance, formGroup);
        fields.add(formGroup);
    }
    
    @Override
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderBooleanField(param, isFirstInstance, formGroup);
        fields.add(formGroup);
    }
    
    @Override
    protected void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderChoiceField(param, isFirstInstance, formGroup);
        fields.add(formGroup);
    }

    
    //
    // Private methods
    //
    private void setOriginalValues(GwtConfigComponent component) {
        for (GwtConfigParameter parameter : component.getParameters()) {
            parameter.setValue(parameter.getValue());
        }
    }
    
    private void apply() {
        if (isValid()) {
            if (isDirty()) {
                // TODO ask for confirmation first
                modal = new Modal();

                ModalHeader header = new ModalHeader();
                header.setTitle(MSGS.confirm());
                modal.add(header);

                ModalBody body = new ModalBody();
                body.add(new Span(MSGS.deviceConfigConfirmation(m_configurableComponent.getComponentName())));
                modal.add(body);

                ModalFooter footer = new ModalFooter();
                ButtonGroup group = new ButtonGroup();
                Button yes = new Button();
                yes.setText(MSGS.yesButton());
                yes.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        EntryClassUi.showWaitModal();
                        try {
                            getUpdatedConfiguration();
                        } catch (Exception ex) {
                            EntryClassUi.hideWaitModal();
                            FailureHandler.handle(ex);
                            return;
                        }
                        gwtXSRFService.generateSecurityToken(new AsyncCallback<GwtXSRFToken>() {

                            @Override
                            public void onFailure(Throwable ex) {
                                EntryClassUi.hideWaitModal();
                                FailureHandler.handle(ex);
                            }

                            @Override
                            public void onSuccess(GwtXSRFToken token) {
                                gwtComponentService.updateComponentConfiguration(token, m_configurableComponent, new AsyncCallback<Void>() {
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        EntryClassUi.hideWaitModal();
                                        FailureHandler.handle(caught);
                                        errorLogger.log(Level.SEVERE, caught.getLocalizedMessage() != null ? caught.getLocalizedMessage() : caught.getClass().getName(), caught);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        modal.hide();
                                        logger.info(MSGS.info() + ": " + MSGS.deviceConfigApplied());
                                        apply.setEnabled(false);
                                        reset.setEnabled(false);
                                        setDirty(false);
                                        entryClass.initServicesTree();
                                        EntryClassUi.hideWaitModal();
                                    }
                                });

                            }
                        });
                    }
                });
                group.add(yes);
                Button no = new Button();
                no.setText(MSGS.noButton());
                no.addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        modal.hide();
                    }
                });
                group.add(no);
                footer.add(group);
                modal.add(footer);
                modal.show();

                // ----

            }                // end isDirty()
        } else {
            errorLogger.log(Level.SEVERE, "Device configuration error!");
            incompleteFieldsModal.show();
        }                // end else isValid
    }

    private GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = fields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }
        return m_configurableComponent;
    }

    private void initInvalidDataModal() {
        incompleteFieldsModal.setTitle(MSGS.warning());
        incompleteFieldsText.setText(MSGS.formWithErrorsOrIncomplete());
    }
}
