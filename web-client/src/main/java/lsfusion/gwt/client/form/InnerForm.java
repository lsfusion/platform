package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GInnerComponent;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

public class InnerForm extends FormContainer {
    private final Integer inContainerId;

    public InnerForm(FormsController formsController, boolean async, Event editEvent, Integer inContainerId) {
        super(formsController, async, editEvent);
        this.inContainerId = inContainerId;
    }

    private Widget widget;

    @Override
    protected void setContent(Widget widget) {
        setFormContent(widget);
        this.widget = widget;
    }

    GContainer inContainer;
    GInnerComponent innerComponent;

    protected void setFormContent(Widget widget) {
        FormContainer formContainer = MainFrame.getCurrentForm();
        if(formContainer != null) {
            GFormController formController = formContainer.getForm();

            innerComponent = new GInnerComponent();
            innerComponent.setFlex(1); //без flex - нулевая высота

            inContainer = formController.getForm().findContainerByID(inContainerId);
            inContainer.add(innerComponent);

            formController.getFormLayout().addBaseComponent(innerComponent, widget, null);
        }
    }

    @Override
    public void hide(EndReason editFormCloseReason) {
        FormContainer formContainer = MainFrame.getCurrentForm();
        if(formContainer != null) {
            formContainer.getForm().getFormLayout().removeBaseComponent(innerComponent);
            inContainer.removeFromChildren(innerComponent);
        }
    }

    @Override
    protected void setCaption(String caption, String tooltip) {
    }

    @Override
    public GWindowFormType getWindowType() {
        return null;
    }

    @Override
    public void show(GAsyncFormController asyncFormController, Integer index) {
        if (!async)
            onSyncFocus(true);
    }

    @Override
    protected Element getFocusedElement() {
        return widget.getElement();
    }
}


