package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.GFormComponent;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.design.view.TabbedContainerView;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GContainerWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

public class ContainerForm extends FormContainer {
    private final String caption;
    private final Integer inContainerId;

    public ContainerForm(FormsController formsController, String caption, boolean async, Event editEvent, Integer inContainerId) {
        super(formsController, async, editEvent);
        this.caption = caption;
        this.inContainerId = inContainerId;
    }

    private Widget widget;

    @Override
    protected void setContent(Widget widget) {
        setFormContent(widget);
        this.widget = widget;
    }

    private GContainer inContainer;
    private GFormComponent innerComponent;

    protected void setFormContent(Widget widget) {
        FormContainer formContainer = MainFrame.getCurrentForm();
        if(formContainer != null) {
            GFormController formController = formContainer.getForm();

            innerComponent = new GFormComponent(caption);

            inContainer = formController.getForm().findContainerByID(inContainerId);
            inContainer.add(innerComponent);

            formController.putContainerForm(this);

            GFormLayout layout = formController.getFormLayout();
            layout.addBaseComponent(innerComponent, widget, null);
            layout.update(-1);
            if(inContainer.tabbed)
                ((TabbedContainerView)layout.getContainerView(inContainer)).activateTab(innerComponent);
        }
    }

    @Override
    public void hide(EndReason editFormCloseReason) {
        FormContainer formContainer = MainFrame.getCurrentForm();
        if(formContainer != null) {
            GFormController formController = formContainer.getForm();
            formController.removeContainerForm(this);

            GFormLayout layout = formController.getFormLayout();
            layout.removeBaseComponent(innerComponent);
            inContainer.removeFromChildren(innerComponent);
            if(inContainer.tabbed)
                ((TabbedContainerView)layout.getContainerView(inContainer)).activateLastTab();
        }
    }

    @Override
    protected void setCaption(String caption, String tooltip) {
    }

    @Override
    public GWindowFormType getWindowType() {
        return new GContainerWindowFormType(inContainerId);
    }

    @Override
    public void show(GAsyncFormController asyncFormController) {
        if (!async)
            onSyncFocus(true);
    }

    @Override
    protected Element getFocusedElement() {
        return widget.getElement();
    }
}


