package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

import java.util.function.Consumer;

public class FormEmbedded extends FormContainer {

    private final GFormController contextForm;

    private class FormCellEditor implements RequestReplaceCellEditor {
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
        public boolean checkEnterEvent(Event event) {
            return event.getCtrlKey();
        }

        @Override
        public void start(Event editEvent, Element parent, Object oldValue) {
        }

        @Override
        public void onBlur(Event event, Element parent) {
            // the problem is when form is embedded and elements that have focus in DOM are removed (for example clearRender is called) focus moves somewhere, and that causes blur event, which embed cell editor treats as finish editing (which is not what we want)
            // so we just delay execution, expecting that the one who lost focus will immediately restore it
            // it seems that later this scheme should be used for all onBlur events
            if(event.getRelatedEventTarget() == null) {
                SmartScheduler.getInstance().scheduleDeferred(() -> {
                    if(GwtClientUtils.getFocusedChild(parent) == null)
                        RequestReplaceCellEditor.super.onBlur(event, parent);
                });
            } else {
                RequestReplaceCellEditor.super.onBlur(event, parent);
            }
        }

        @Override
        public void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
//            removeContent();

            RequestReplaceCellEditor.super.clearRender(cellParent, renderContext, cancel);
        }

        @Override
        public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize, Object oldValue) {
            renderElement = cellParent;
        }
    }

    private Element renderElement;

    public FormEmbedded(FormsController formsController, long editRequestIndex, boolean async, Event editEvent, EditContext editContext, GFormController contextForm) {
        super(formsController, async, editEvent);

        this.contextForm = contextForm;

        contextForm.edit(new FormCellEditor(), editEvent, null, (result, commitReason) -> {}, (result, commitReason) -> {}, (cancelReason) -> {}, editContext, "", async ? editRequestIndex : -1);
    }

    @Override
    public GWindowFormType getWindowType() {
        return GWindowFormType.EMBEDDED;
    }

    @Override
    protected Element getFocusedElement() {
        return renderElement;
    }

    @Override
    public void initForm(FormsController formsController, GForm gForm, Consumer<EndReason> hiddenHandler, boolean isDialog, boolean autoSize) {
        super.initForm(formsController, gForm, hiddenHandler, isDialog, autoSize);

        form.contextEditForm = contextForm;
        form.getWidget().getElement().setTabIndex(-1); // we need to make form focusable, because otherwise clicking on this form will lead to moving focus to the grid (not the cell), which will cause blur and stop editing
    }

    private Widget widget;
    @Override
    protected void setContent(Widget widget) {
        removeContent();

        getAttachContainer().add(widget);

        Element element = widget.getElement();
        GwtClientUtils.setupPercentParent(element);
        renderElement.appendChild(element);

        this.widget = widget;
    }

    private void removeContent() {
        if(this.widget != null) {
            ResizableComplexPanel attachContainer = getAttachContainer();
            attachContainer.getElement().appendChild(this.widget.getElement());
            attachContainer.remove(this.widget);
        }
    }

    private ResizableComplexPanel getAttachContainer() {
        return contextForm.formLayout.recordViews;
    }

    private FormContainer containerForm;
//    private FormContainer prevForm;

    @Override
    protected FormContainer getContainerForm() {
//        return this;
        return containerForm;
    }

    @Override
    public void show() {
//        prevForm = MainFrame.getAssertCurrentForm();
//        if(prevForm != null) // if there were no currentForm
//            prevForm.onBlur(false);

//        onFocus(true);

        // we don't need to change currentForm for embedded form, since if closes on focus lost, so we don't need notifications / global key events
        // for the same reason we don't need to do onBlur
        // however now it's hard to tell what is the right approach
        if(!async)
            onSyncFocus(true);
        else
            containerForm = MainFrame.getAssertCurrentForm();
    }

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
    private void finishEditing(EndReason editFormCloseReason) {
        assert !finishedEditing;

//        renderElement.setTabIndex(-1);
        if(!async) {
            form.contextEditForm = null; // it's important to do before removeContent (to prevent propagateFocusEvent while removing content)
            form.checkCommitEditing(); // we need to check commit editing, otherwise form will be in editing mode, and for example ClosePressed won't be flushed
        }
        removeContent();

        if(editFormCloseReason instanceof CommitReason)
            contextForm.commitEditing(new GUserInputResult(), (CommitReason) editFormCloseReason);
        else
            contextForm.cancelEditing((CancelReason) editFormCloseReason);

        finishedEditing = true;
    }

    @Override
    protected void setCaption(String caption, String tooltip) {
    }
}
