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
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GContainerWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;

public class ContainerForm extends WidgetForm {
    private final GContainerWindowFormType windowType;

    public ContainerForm(FormsController formsController, GFormController contextForm, boolean async, Event editEvent, GContainerWindowFormType windowType) {
        super(formsController, contextForm, async, editEvent, GFormLayout.createContainerCaptionWidget(null, getInContainer(contextForm, windowType), false, true));

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
        // out of the form it is shown in, not the current one: a CLOSE FORM another form runs, or a modal popup open,
        // leaves another form current, or none
        contextForm.removeContainerForm(this);

        GFormLayout layout = contextForm.getFormLayout();
        layout.removeBaseComponent(innerComponent);
        inContainer.removeFromChildren(innerComponent);
        if(inContainer.tabbed) {
            TabbedContainerView containerView = ((TabbedContainerView)layout.getContainerView(inContainer));
            if(containerView != null) {
                containerView.activateLastTab();
            }
        }
        // as show does: a container the form leaves with nothing shown is hidden, and its tab in an outer tabbed
        // container with it - once the tab shown in the form's place is chosen: updated before, the layout chose one
        // itself
        layout.update(-1);
    }

    private static GContainer getInContainer(GFormController formController, GContainerWindowFormType windowType) {
        return formController.getForm().findContainerByID(windowType.getInContainerId());
    }

    @Override
    public void show(GAsyncFormController asyncFormController) {
        innerComponent = new GFormComponent();

        inContainer = getInContainer(contextForm, windowType);
        inContainer.add(innerComponent);

        contextForm.addContainerForm(this);

        GFormLayout layout = contextForm.getFormLayout();
        layout.addBaseComponent(innerComponent, new ComponentWidget(contentWidget, captionWidget), null);
        layout.update(-1);
        if(inContainer.tabbed)
            ((TabbedContainerView)layout.getContainerView(inContainer)).activateTab(innerComponent);

        if (!async)
            onSyncFocus(true);
    }
}


