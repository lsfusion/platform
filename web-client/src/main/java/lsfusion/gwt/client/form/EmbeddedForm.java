package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.controller.SmartScheduler;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
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
        public void render(Element cellParent, RenderContext renderContext, PValue oldValue, Integer renderedWidth, Integer renderedHeight) {
            renderElement = cellParent;
        }
    }

    @Override
    protected CellEditor createCellEditor() {
        return new EmbeddedCellEditor();
    }

    @Override
    public Element getContentElement() {
        return renderElement;
    }

    private Element renderElement;

//    private boolean autoSize;

    public EmbeddedForm(FormsController formsController, GFormController contextForm, long editRequestIndex, boolean async, Event editEvent, EditContext editContext) {
        super(formsController, contextForm, editRequestIndex, async, editEvent, editContext);

//        autoSize = editContext.getProperty().autoSize;
    }

    private ResizableComplexPanel getAttachContainer() {
        return contextForm.formLayout.attachContainer;
    }

    private static final String FORM_EMBEDDED = "form_embedeed";
    public static boolean is(Element element) {
        return element.getPropertyBoolean(FORM_EMBEDDED);
    }

    @Override
    protected void setFormContent(Widget widget) {
        getAttachContainer().add(widget);

        Element element = widget.getElement();
        GwtClientUtils.setupPercentParent(element);
        //        if(!autoSize) {
//            element.addClassName("comp-shrink-horz");
//            element.addClassName("comp-shrink-vert");
//        }
        element.addClassName("form-embedded");
        element.setPropertyBoolean(FORM_EMBEDDED, true);

        renderElement.appendChild(element);
    }

    @Override
    protected void onSyncFocus(boolean add) {
        super.onSyncFocus(add);
        if(add && editEvent != null) {
            DOM.dispatchEvent(editEvent, FocusUtils.getFocusedElement());
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
        return GModalityWindowFormType.EMBEDDED;
    }

    private FormContainer containerForm;

    @Override
    protected FormContainer getContainerForm() {
        return containerForm;
    }

    @Override
    public void show(GAsyncFormController asyncFormController) {
        // we don't need to change currentForm for embedded form, since if closes on focus lost, so we don't need notifications / global key events
        // for the same reason we don't need to do onBlur
        // however now it's hard to tell what is the right approach
        if(!async)
            onSyncFocus(true);
        else
            containerForm = MainFrame.getAssertCurrentForm();
    }

    @Override
    public void initForm(FormsController formsController, GForm gForm, BiConsumer<GAsyncFormController, EndReason> hiddenHandler, boolean isDialog, int dispatchPriority, String formId) {
        super.initForm(formsController, gForm, hiddenHandler, isDialog, dispatchPriority, formId);

        form.getWidget().getElement().setTabIndex(-1); // we need to make form focusable, because otherwise clicking on this form will lead to moving focus to the grid (not the cell), which will cause blur and stop editing
    }
}
