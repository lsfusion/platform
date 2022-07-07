package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GWindowFormType;

public class InnerForm extends FormContainer {

    private String inFormCanonicalName;
    private Integer inComponentId;

    protected final GFormController contextForm;

    public InnerForm(FormsController formsController, boolean async, Event editEvent, GFormController contextForm, String inFormCanonicalName, Integer inComponentId) {
        super(formsController, async, editEvent);

        this.contextForm = contextForm;
        this.inFormCanonicalName = inFormCanonicalName;
        this.inComponentId = inComponentId;
    }

    private Widget widget;

    @Override
    protected void setContent(Widget widget) {
        setFormContent(widget);
        this.widget = widget;
    }

    protected void setFormContent(Widget widget) {
        GFormController formController = formsController.findForm(inFormCanonicalName).getForm();

        GContainer container = formController.getForm().findContainerByID(inComponentId);

        container.setFlex(1);
        container.setAlignment(GFlexAlignment.STRETCH);

        formController.getFormLayout().addBaseComponent(container, widget, null);
    }

    @Override
    public void hide(EndReason editFormCloseReason) {
        widget.removeFromParent();
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


