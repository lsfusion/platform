package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.property.cell.classes.controller.PopupCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.view.MainFrame;

public class PopupForm extends EditingForm {

    private class PopupFormCellEditor extends CellEditor implements PopupCellEditor {

        @Override
        public void enterPressed(Element parent) {
            commit(parent,  CommitReason.ENTERPRESSED);
        }

        @Override
        public boolean commitOnAutoClose() {
            return true;
        }

        protected PopupDialogPanel popup;

        @Override
        public boolean removeBorder() {
            return true;
        }

        @Override
        public void setPopup(PopupDialogPanel popup) {
            this.popup = popup;
        }

        @Override
        public PopupDialogPanel getPopup() {
            return popup;
        }

        @Override
        public void start(Event editEvent, Element parent, Object oldValue) {
            PopupCellEditor.super.start(editEvent, parent, oldValue);

            if(DataGrid.isMouseEvent(editEvent)) {
                clientX = editEvent.getClientX();
                clientY = editEvent.getClientY();
            } else {
                clientX = parent.getAbsoluteLeft();
                clientY = parent.getAbsoluteTop();
            }
        }

        @Override
        public void stop(Element parent, boolean cancel, boolean blurred) {
            onBlur(true);

            // actual hide
            PopupCellEditor.super.stop(parent, cancel, blurred);

            if(prevForm != null)
                prevForm.onFocus(false);
        }
    }

    private int clientX;
    private int clientY;

    private FormContainer prevForm;

    @Override
    public void show(Long requestIndex, Integer index) {
        prevForm = MainFrame.getAssertCurrentForm();
        if(prevForm != null) // if there were no currentForm
            prevForm.onBlur(false);

        GwtClientUtils.showPopup(getPopup(), clientX, clientY);

        onFocus(true);
    }

    @Override
    protected void setFormContent(Widget widget) {
        getPopup().setWidget(widget);
    }

    @Override
    protected void removeFormContent(Widget widget) {
        getPopup().setWidget(null);
    }

    private PopupFormCellEditor cellEditor;
    @Override
    protected CellEditor createCellEditor() {
        cellEditor = new PopupFormCellEditor();
        return cellEditor;
    }

    protected PopupDialogPanel getPopup() {
        return cellEditor.popup;
    }

    @Override
    protected Element getFocusedElement() {
        return getPopup().getElement();
    }

    public PopupForm(FormsController formsController, long editRequestIndex, boolean async, Event editEvent, EditContext editContext, GFormController contextForm) {
        super(formsController, editRequestIndex, async, editEvent, editContext, contextForm);
    }

    @Override
    public GWindowFormType getWindowType() {
        return GWindowFormType.POPUP;
    }
}
