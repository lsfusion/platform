package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.window.GWindowFormType;

public abstract class EditingForm extends FormContainer {

    protected class CellEditor implements RequestCellEditor {

        public boolean checkEnterEvent(NativeEvent event) {
            return event.getCtrlKey();
        }

        @Override
        public void commit(Element parent, CommitReason commitReason) {
            // we need to finish editing immediately, since another editing might be started, and there is an assertion, that there is no editing (and other checkCommitEditing branches have the same asserting)
            finishEditing(commitReason);

            closePressed(commitReason);
        }

        @Override
        public void cancel(Element parent, CancelReason cancelReason) {
            finishEditing(cancelReason);

            closePressed(cancelReason);
        }

        @Override
        public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
        }
    }

    protected abstract CellEditor createCellEditor();

    public EditingForm(FormsController formsController, GFormController contextForm, long editRequestIndex, boolean async, Event editEvent, EditContext editContext) {
        super(formsController, contextForm, async, editEvent);

        contextForm.edit(createCellEditor(), editEvent != null ? new EventHandler(editEvent) : null, null, (result, commitReason) -> {}, (result, commitReason) -> {}, (cancelReason) -> {}, editContext, "", async ? editRequestIndex : -1);
    }

    @Override
    public abstract GWindowFormType getWindowType();

    private Widget widget;
    @Override
    protected void setContent(Widget widget) {
        removeContent();

        setFormContent(widget);

        this.widget = widget;
    }

    protected abstract void setFormContent(Widget widget);

    private void removeContent() {
        if(this.widget != null) {
            removeFormContent(this.widget);
        }
    }

    protected abstract void removeFormContent(Widget widget);

    @Override
    public void hide(EndReason editFormCloseReason) {
//        if(MainFrame.getAssertCurrentForm() == this) { // because actually it's not modal, so someone can change currentForm already
//            onBlur(true);
//
//            if (prevForm != null)
//                prevForm.onFocus(false);
//        }

        assert finishedEditing == (editFormCloseReason != null);
        if(!finishedEditing)
            finishEditing(editFormCloseReason);
    }

    private boolean finishedEditing;
    protected void finishEditing(EndReason editFormCloseReason) {
        assert !finishedEditing;

        if(!async)
            form.checkCommitEditing(); // we need to check commit editing, otherwise form will be in editing mode, and for example ClosePressed won't be flushed
        removeContent();

        if(editFormCloseReason instanceof CommitReason)
            contextForm.commitEditing(new GUserInputResult(), (CommitReason) editFormCloseReason);
        else
            contextForm.cancelEditing((CancelReason) editFormCloseReason);

        finishedEditing = true;
    }

    @Override
    public Widget getCaptionWidget() {
        return null;
    }
}
