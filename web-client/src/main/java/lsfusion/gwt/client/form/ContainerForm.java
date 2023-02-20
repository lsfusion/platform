package lsfusion.gwt.client.form;

import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFormComponent;
import lsfusion.gwt.client.form.design.view.ComponentWidget;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.design.view.TabbedContainerView;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GContainerWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

public class ContainerForm extends WidgetForm {
    private final GFormController formController;

    private final GContainerWindowFormType windowType;

    public ContainerForm(FormsController formsController, boolean async, Event editEvent, GFormController formController, GContainerWindowFormType windowType) {
        super(formsController, async, editEvent, GFormLayout.createContainerCaptionWidget(getInContainer(formController, windowType), true));

        this.formController = formController;
        this.windowType = windowType;
    }

    @Override
    protected void onMaskClick() {
    }

    @Override
    public GWindowFormType getWindowType() {
        return windowType;
    }

    private GContainer inContainer;
    private GFormComponent innerComponent;

    @Override
    public void hide(EndReason editFormCloseReason) {
        FormContainer formContainer = MainFrame.getCurrentForm();
        if(formContainer != null) {
            GFormController formController = formContainer.getForm();
            assert formController.equals(this.formController); // ?? to remove later

            formController.removeContainerForm(this);

            GFormLayout layout = formController.getFormLayout();
            layout.removeBaseComponent(innerComponent);
            inContainer.removeFromChildren(innerComponent);
            if(inContainer.tabbed) {
                TabbedContainerView containerView = ((TabbedContainerView)layout.getContainerView(inContainer));
                if(containerView != null) {
                    containerView.activateLastTab();
                }
            }
        }
    }

    private static GContainer getInContainer(GFormController formController, GContainerWindowFormType windowType) {
        return formController.getForm().findContainerByID(windowType.getInContainerId());
    }

    @Override
    public void show(GAsyncFormController asyncFormController) {
        innerComponent = new GFormComponent();

        inContainer = getInContainer(formController, windowType);
        inContainer.add(innerComponent);

        formController.addContainerForm(this);

        GFormLayout layout = formController.getFormLayout();
        layout.addBaseComponent(innerComponent, new ComponentWidget(contentWidget, captionWidget), null);
        layout.update(-1);
        if(inContainer.tabbed)
            ((TabbedContainerView)layout.getContainerView(inContainer)).activateTab(innerComponent);

        if (!async)
            onSyncFocus(true);
    }
}


