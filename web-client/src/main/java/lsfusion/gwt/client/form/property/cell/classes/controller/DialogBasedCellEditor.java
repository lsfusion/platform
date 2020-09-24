package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.WindowBox;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.WindowCellEditor;

public abstract class DialogBasedCellEditor implements WindowCellEditor {
    protected final EditManager editManager;
    protected final GPropertyDraw property;

    protected final WindowBox dialog;

    protected final int width;
    protected final int height;

    public DialogBasedCellEditor(EditManager editManager, GPropertyDraw property, String title, int width, int height) {
        this.editManager = editManager;
        this.property = property;
        this.width = width;
        this.height = height;
        
        dialog = new WindowBox(false, true, true) {
            @Override
            protected void onCloseClick(ClickEvent event) {
                DialogBasedCellEditor.this.onCloseClick();
            }
        };
        dialog.setText(title);
        dialog.setModal(true);
        dialog.setGlassEnabled(true);
    }

    protected void onCloseClick() {
        cancelEditing();
    }

    @Override
    public void startEditing(Event editEvent, Element parent, Object oldValue) {
        Widget content = createComponent(editEvent, parent, oldValue);
        if (width != -1 && height != -1) {
            content.setPixelSize(width, height);
        }
        dialog.setWidget(content);
        dialog.center();
        afterStartEditing();
    }

    protected void afterStartEditing() {
    }

    protected final void commitEditing(Object value) {
        dialog.hide();
        editManager.commitEditing(value);
    }

    protected final void cancelEditing() {
        dialog.hide();
        editManager.cancelEditing();
    }

    protected abstract Widget createComponent(Event editEvent, Element parent, Object oldValue);
}
