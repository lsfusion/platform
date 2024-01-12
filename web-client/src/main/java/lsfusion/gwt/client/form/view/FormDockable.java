package lsfusion.gwt.client.form.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.WidgetForm;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;

public final class FormDockable extends WidgetForm {
    private String canonicalName;

    private final WidgetForm.CloseButton closeButton;

    private FormDockable blockingForm; //GFormController

    @Override
    public GWindowFormType getWindowType() {
        return GModalityWindowFormType.DOCKED;
    }

    @Override
    public Element getFocusedElement() {
        return contentWidget.getElement();
    }

    Result<JavaScriptObject> popup = new Result<>();
    public FormDockable(FormsController formsController, GFormController contextForm, String canonicalName, boolean async, Event editEvent) {
        super(formsController, contextForm, async, editEvent, GFormLayout.createTabCaptionWidget());

        this.canonicalName = canonicalName;

        captionWidget.addDomHandler(event -> {
            GwtClientUtils.stopPropagation(event);

            final MenuBar menuBar = new MenuBar(true);
            menuBar.addItem(new MenuItem(ClientMessages.Instance.get().closeAllTabs(), () -> {
                GwtClientUtils.hideTippyPopup(popup.result);
                formsController.closeAllTabs();
            }));

            popup.result = GwtClientUtils.showTippyPopup(getTabWidget(), getTabWidget().getElement(), menuBar);
        }, ContextMenuEvent.getType());

        closeButton = new WidgetForm.CloseButton();
    }

    @Override
    public void show(GAsyncFormController asyncFormController) {
        showDockable(null);
    }

    public void showDockable(Integer index) {
        formsController.addDockable(this, index);
    }

    @Override
    public void hide(EndReason editFormCloseReason) {
        formsController.removeDockable(this);
    }

    public void setBlockingForm(FormDockable blocking) {
        blockingForm = blocking;
    }

    public Widget getTabWidget() {
        return captionWidget;
    }

    public Widget getCloseButton() {
        return closeButton;
    }

    public FlexPanel getContentWidget() {
        return contentWidget;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    @Override
    public void block() {
        super.block();

        closeButton.setEnabled(false);
    }

    @Override
    public void unblock() {
        super.unblock();

        closeButton.setEnabled(true);
    }

    protected void onMaskClick() {
        Widget content = contentWidget.getContent();
        if (content instanceof GFormLayout && blockingForm != null) {
            ((GFormLayout) content).getFormsController().selectTab(blockingForm);
        }
    }
}
