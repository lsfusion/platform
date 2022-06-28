package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

import java.util.function.BiConsumer;

public class EmbeddedForm extends EditingForm {

    private class EmbeddedCellEditor extends CellEditor implements RequestReplaceCellEditor {

        @Override
        public boolean checkEnterEvent(Event event) {
            return checkEnterEvent((NativeEvent)event);
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
        public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize, Object oldValue) {
            renderElement = cellParent;
        }
    }

    @Override
    protected CellEditor createCellEditor() {
        return new EmbeddedCellEditor();
    }

    @Override
    protected Element getFocusedElement() {
        return renderElement;
    }

    private Element renderElement;

    public EmbeddedForm(FormsController formsController, long editRequestIndex, boolean async, Event editEvent, EditContext editContext, GFormController contextForm) {
        super(formsController, editRequestIndex, async, editEvent, editContext, contextForm);
    }

    private ResizableComplexPanel getAttachContainer() {
        return contextForm.formLayout.attachContainer;
    }

    @Override
    protected void setFormContent(Widget widget) {
        getAttachContainer().add(widget);

        Element element = widget.getElement();
        GwtClientUtils.setupPercentParent(element);
        renderElement.appendChild(element);
    }

    @Override
    protected void onSyncFocus(boolean add) {
        super.onSyncFocus(add);
        if(add) {
            Element focusedElement = GwtClientUtils.getFocusedElement();
            DOM.dispatchEvent(editEvent, focusedElement);
        }
    }

    @Override
    protected void removeFormContent(Widget widget) {
        ResizableComplexPanel attachContainer = getAttachContainer();
        attachContainer.getElement().appendChild(widget.getElement());
        attachContainer.remove(widget);
    }

    @Override
    public GWindowFormType getWindowType() {
        return GWindowFormType.EMBEDDED;
    }

    private FormContainer containerForm;

    @Override
    protected FormContainer getContainerForm() {
        return containerForm;
    }

    @Override
    public void show(GwtActionDispatcher dispatcher, Long requestIndex, Integer index) {
        // we don't need to change currentForm for embedded form, since if closes on focus lost, so we don't need notifications / global key events
        // for the same reason we don't need to do onBlur
        // however now it's hard to tell what is the right approach
        if(!async)
            onSyncFocus(true);
        else
            containerForm = MainFrame.getAssertCurrentForm();
    }

    @Override
    public void initForm(FormsController formsController, GForm gForm, BiConsumer<GAsyncFormController, EndReason> hiddenHandler, boolean isDialog, boolean autoSize) {
        super.initForm(formsController, gForm, hiddenHandler, isDialog, autoSize);

        form.contextEditForm = contextForm;
        form.getWidget().getElement().setTabIndex(-1); // we need to make form focusable, because otherwise clicking on this form will lead to moving focus to the grid (not the cell), which will cause blur and stop editing
    }

    @Override
    protected void finishEditing(EndReason editFormCloseReason) {
        if(!async)
            form.contextEditForm = null; // it's important to do before removeContent (to prevent propagateFocusEvent while removing content)

        super.finishEditing(editFormCloseReason);
    }
}
