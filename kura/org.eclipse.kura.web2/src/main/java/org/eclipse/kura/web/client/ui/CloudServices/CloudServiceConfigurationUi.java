package org.eclipse.kura.web.client.ui.CloudServices;

import java.util.Iterator;
import java.util.logging.Level;

import org.eclipse.kura.web.client.ui.AbstractServicesUi;
import org.eclipse.kura.web.client.ui.EntryClassUi;
import org.eclipse.kura.web.client.util.FailureHandler;
import org.eclipse.kura.web.shared.model.GwtConfigComponent;
import org.eclipse.kura.web.shared.model.GwtConfigParameter;
import org.eclipse.kura.web.shared.model.GwtXSRFToken;
import org.eclipse.kura.web.shared.service.GwtComponentService;
import org.eclipse.kura.web.shared.service.GwtComponentServiceAsync;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenService;
import org.eclipse.kura.web.shared.service.GwtSecurityTokenServiceAsync;
import org.gwtbootstrap3.client.ui.Alert;
import org.gwtbootstrap3.client.ui.Button;
import org.gwtbootstrap3.client.ui.ButtonGroup;
import org.gwtbootstrap3.client.ui.FieldSet;
import org.gwtbootstrap3.client.ui.Form;
import org.gwtbootstrap3.client.ui.FormGroup;
import org.gwtbootstrap3.client.ui.Modal;
import org.gwtbootstrap3.client.ui.ModalBody;
import org.gwtbootstrap3.client.ui.ModalFooter;
import org.gwtbootstrap3.client.ui.ModalHeader;
import org.gwtbootstrap3.client.ui.html.Span;
import org.gwtbootstrap3.client.ui.html.Text;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

public class CloudServiceConfigurationUi extends AbstractServicesUi {

    private static ServiceConfigurationUiUiBinder uiBinder = GWT.create(ServiceConfigurationUiUiBinder.class);

    private final GwtSecurityTokenServiceAsync gwtXSRFService = GWT.create(GwtSecurityTokenService.class);
    private final GwtComponentServiceAsync gwtComponentService = GWT.create(GwtComponentService.class);

    private boolean dirty, initialized;

    interface ServiceConfigurationUiUiBinder extends UiBinder<Widget, CloudServiceConfigurationUi> {
    }

    private Modal modal;
    private GwtConfigComponent originalConfig;

    @UiField
    Button applyConnectionEdit;
    @UiField
    Button resetConnectionEdit;
    @UiField
    FieldSet connectionEditFields;
    @UiField
    Form connectionEditField;
    @UiField
    Modal incompleteFieldsModal;
    @UiField
    Alert incompleteFields;
    @UiField
    Text incompleteFieldsText;

    public CloudServiceConfigurationUi(final GwtConfigComponent addedItem) {
        initWidget(uiBinder.createAndBindUi(this));
        initialized = false;
        originalConfig = addedItem;
        restoreConfiguration(originalConfig);
        connectionEditFields.clear();

        applyConnectionEdit.setText(MSGS.apply());
        applyConnectionEdit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                apply();
            }
        });

        resetConnectionEdit.setText(MSGS.reset());
        resetConnectionEdit.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                reset();
            }
        });

        setDirty(false);
        applyConnectionEdit.setEnabled(false);
        resetConnectionEdit.setEnabled(false);
    }

    @Override
    protected void setDirty(boolean flag) {
        dirty = flag;
        if (dirty && initialized) {
            applyConnectionEdit.setEnabled(true);
            resetConnectionEdit.setEnabled(true);
        }
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    protected void reset() {
        if (isDirty()) {
            // Modal
            showDirtyModal();
        }    // end is dirty
    }

    @Override
    protected void renderForm() {
        connectionEditFields.clear();
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
        connectionEditFields.add(formGroup);
    }

    @Override
    protected void renderPasswordField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderPasswordField(param, isFirstInstance, formGroup);
        connectionEditFields.add(formGroup);
    }

    @Override
    protected void renderBooleanField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderBooleanField(param, isFirstInstance, formGroup);
        connectionEditFields.add(formGroup);
    }

    @Override
    protected void renderChoiceField(final GwtConfigParameter param, boolean isFirstInstance, FormGroup formGroup) {
        super.renderChoiceField(param, isFirstInstance, formGroup);
        connectionEditFields.add(formGroup);
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
                                gwtComponentService.updateComponentConfiguration(token, m_configurableComponent,
                                        new AsyncCallback<Void>() {

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        EntryClassUi.hideWaitModal();
                                        FailureHandler.handle(caught);
                                        errorLogger.log(
                                                Level.SEVERE, caught.getLocalizedMessage() != null
                                                        ? caught.getLocalizedMessage() : caught.getClass().getName(),
                                                caught);
                                    }

                                    @Override
                                    public void onSuccess(Void result) {
                                        modal.hide();
                                        logger.info(MSGS.info() + ": " + MSGS.deviceConfigApplied());
                                        applyConnectionEdit.setEnabled(false);
                                        resetConnectionEdit.setEnabled(false);
                                        setDirty(false);
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

            }                     // end isDirty()
        } else {
            errorLogger.log(Level.SEVERE, "Device configuration error!");
            incompleteFieldsModal.show();
        }                     // end else isValid
    }

    private GwtConfigComponent getUpdatedConfiguration() {
        Iterator<Widget> it = connectionEditFields.iterator();
        while (it.hasNext()) {
            Widget w = it.next();
            if (w instanceof FormGroup) {
                FormGroup fg = (FormGroup) w;
                fillUpdatedConfiguration(fg);
            }
        }
        return m_configurableComponent;
    }
    
    private void showDirtyModal() {
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
                resetVisualization();
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
    }
    
    protected void resetVisualization() {
        restoreConfiguration(originalConfig);
        renderForm();
        applyConnectionEdit.setEnabled(false);
        resetConnectionEdit.setEnabled(false);
        setDirty(false);
    }
}
