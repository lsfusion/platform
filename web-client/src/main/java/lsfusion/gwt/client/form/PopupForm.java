package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
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
            if(handler != null && DataGrid.isMouseEvent(event = handler.event)) {
                parentElement = Element.as(event.getEventTarget());
            } else {
                parentElement = parent;
            }
        }

        @Override
        public boolean checkEnterEvent(NativeEvent event) {
            return GKeyStroke.isPlainKeyEvent(event);
        }

        @Override
        public void stop(Element parent, boolean cancel, boolean blurred) {
            onBlur(true);

            if(prevForm != null)
                prevForm.onFocus(false);
        }
    }

    protected Widget formWidget;
    private Element parentElement;

    private FormContainer prevForm;

    @Override
    public void show(GAsyncFormController asyncFormController) {
        prevForm = MainFrame.getAssertCurrentForm();
        if(prevForm != null) // if there were no currentForm
            prevForm.onBlur(false);

        GwtClientUtils.showTippyPopup(null, parentElement, formWidget, () -> cellEditor.commit(parentElement, CommitReason.FORCED));

        onFocus(true);
    }

    @Override
    protected void setFormContent(Widget widget) {
        this.formWidget = widget;
    }

    @Override
    protected void removeFormContent(Widget widget) {
        this.formWidget = null;
    }

    private PopupFormCellEditor cellEditor;
    @Override
    protected CellEditor createCellEditor() {
        cellEditor = new PopupFormCellEditor();
        return cellEditor;
    }

    @Override
    public Element getFocusedElement() {
        return formWidget.getElement();
    }

    public PopupForm(FormsController formsController, long editRequestIndex, boolean async, Event editEvent, EditContext editContext, GFormController contextForm) {
        super(formsController, editRequestIndex, async, editEvent, editContext, contextForm);
    }

    @Override
    public GWindowFormType getWindowType() {
        return GModalityWindowFormType.POPUP;
    }
}
