package lsfusion.gwt.client.form;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.PopupOwner;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

public class PopupForm extends EditingForm {

    private class PopupFormCellEditor extends CellEditor {

        @Override
        public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
            Event event;
            parentElement = parent;
            if(handler != null && DataGrid.isMouseEvent(event = handler.event)) {
                popupElement = Element.as(event.getEventTarget());
            } else {
                popupElement = parent;
            }
        }

        @Override
        public boolean checkEnterEvent(NativeEvent event) {
            return GKeyStroke.isPlainKeyEvent(event);
        }

        @Override
        public void stop(Element parent, boolean cancel, boolean blurred) {
            super.stop(parent, cancel, blurred);

            onBlur(true);

            if(prevForm != null)
                prevForm.onFocus(false);
        }
    }

    protected Widget contentWidget;
    private Element parentElement;
    private Element popupElement;

    private FormContainer prevForm;

    JavaScriptObject tippy;

    @Override
    public void show(GAsyncFormController asyncFormController) {
        prevForm = MainFrame.getAssertCurrentForm();
        if (prevForm != null) // if there were no currentForm
            prevForm.onBlur(false);

        tippy = GwtClientUtils.showTippyPopup(new PopupOwner(popupOwnerWidget, popupElement), contentWidget, () -> {
            cellEditor.commit(parentElement);
        });

        onFocus(true);
    }

    @Override
    protected void finishEditing(EndReason editFormCloseReason) {
        // NOT hidden here: finishing may itself have been entered from the popup's own hide, and hiding again from
        // inside that hide would run its commit a second time, before the edit is marked finished. Destroying takes
        // the popup off the screen just as well - and it is done last, after the edit is finished, because it detaches
        // the form's content and doing that mid-edit would tear it out from under the edit being finished
        JavaScriptObject closing = tippy;
        tippy = null;

        super.finishEditing(editFormCloseReason);

        // deferred, because finishing may itself have been entered from the popup's own hide, and a popup is not
        // destroyed from inside its hide. Idempotent, so the terminal cleanup that hide schedules and this one are
        // the same thing done once
        if(closing != null)
            Scheduler.get().scheduleDeferred(() -> GwtClientUtils.destroyTippyPopup(closing));
    }

    @Override
    protected void setFormContent(Widget widget) {
        this.contentWidget = widget;
    }

    @Override
    protected void removeFormContent(Widget widget) {
        this.contentWidget = null;
    }

    private PopupFormCellEditor cellEditor;
    @Override
    protected CellEditor createCellEditor() {
        cellEditor = new PopupFormCellEditor();
        return cellEditor;
    }

    @Override
    public Element getContentElement() {
        return contentWidget.getElement();
    }

    public PopupForm(FormsController formsController, GFormController contextForm, long editRequestIndex, boolean async, Event editEvent, EditContext editContext) {
        super(formsController, contextForm, editRequestIndex, async, editEvent, editContext);

        popupOwnerWidget = editContext.getPopupOwnerWidget();
    }

    private final Widget popupOwnerWidget;

    @Override
    public GWindowFormType getWindowType() {
        return GModalityWindowFormType.POPUP;
    }
}
