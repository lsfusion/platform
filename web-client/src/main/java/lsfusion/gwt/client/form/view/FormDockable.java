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
import lsfusion.gwt.client.navigator.window.GWindowFormType;

public final class FormDockable extends WidgetForm {
    private String canonicalName;
    // the open's window type, with the FORMS window it named. What was ASKED for, kept as it was asked: on the
    // mobile layout the form still opens in System.forms, but the type it reports must equal the one its async
    // placeholder reported, or the placeholder is thrown away for a fresh container
    private final GWindowFormType windowType;

    private final WidgetForm.CloseButton closeButton;

    private FormDockable blockingForm; //GFormController
    // the other end of the same pair: this form is the docked-modal child - the blocking form of the form it was
    // opened from, which waits for it. Set where the pointer above is, so that either end can be recognised from the
    // form alone
    private boolean blocksOpener;

    // the window listens for the focus entering this form, on the element that outlives every placement, and a form is
    // placed more than once - so the listener is put on once and this says it has been
    public boolean focusListened;

    @Override
    public GWindowFormType getWindowType() {
        return windowType;
    }

    Result<JavaScriptObject> popup = new Result<>();
    public FormDockable(FormsController formsController, GFormController contextForm, String canonicalName, boolean async, Event editEvent, GWindowFormType windowType) {
        super(formsController, contextForm, async, editEvent, GFormLayout.createTabCaptionWidget());

        this.canonicalName = canonicalName;
        this.windowType = windowType;

        captionWidget.addDomHandler(event -> {
            GwtClientUtils.stopPropagation(event);

            final MenuBar menuBar = new MenuBar(true);
            menuBar.addItem(new MenuItem(ClientMessages.Instance.get().closeAllTabs(), () -> {
                GwtClientUtils.hideAndDestroyTippyPopup(popup.result);
                formsController.getFormsWindow(FormDockable.this).closeAllForms(); // the tabs of THIS window
            }));

            popup.result = GwtClientUtils.showTippyPopup(getTabWidget(), menuBar);
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

    // this form is masked because a docked modal child is open over it; clicking the mask goes to that child
    public boolean hasBlockingForm() {
        return blockingForm != null;
    }

    public boolean isBlockingForm() {
        return blocksOpener;
    }

    // one of the two ends of a docked-modal pair: the form waiting for its child, or the child it waits for. Nothing
    // is asked of either - a blocked form cannot close while its child is open, and the child is there only until it
    // closes, with the window it was aimed at still holding what it held when its opener gets the answer back
    public boolean inBlockingPair() {
        return hasBlockingForm() || isBlockingForm();
    }

    // whether this form could be the keyboard-current one: one still waiting for its own form has none to make
    // current, and one a docked-modal child has masked is not one the user can work in
    public boolean canTakeKeyboard() {
        return !async && !hasBlockingForm();
    }

    public void setBlockingForm(FormDockable blocking) {
        if (blockingForm != null)
            blockingForm.blocksOpener = false;
        blockingForm = blocking;
        if (blocking != null)
            blocking.blocksOpener = true;
    }

    @Override
    protected void arrived(FormsController formsController) {
        formsController.getFormsWindow(this).formArrived(this);
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
            ((GFormLayout) content).getFormsController().setCurrentForm(blockingForm);
        }
    }
}
