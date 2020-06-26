package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.view.WindowBox;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.controller.AbstractGridCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public abstract class DialogBasedGridCellEditor extends AbstractGridCellEditor {
    protected final EditManager editManager;
    protected final GPropertyDraw property;

    protected final WindowBox dialog;

    protected final int width;
    protected final int height;

    public DialogBasedGridCellEditor(EditManager editManager, GPropertyDraw property, String title, int width, int height) {
        this.editManager = editManager;
        this.property = property;
        this.width = width;
        this.height = height;
        
        dialog = new WindowBox(false, true, true) {
            @Override
            protected void onCloseClick(ClickEvent event) {
                DialogBasedGridCellEditor.this.onCloseClick();
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
        Widget content = createComponent(parent, oldValue);
        if (width != -1 && height != -1) {
            content.setPixelSize(width, height);
        }
        dialog.setWidget(content);
        dialog.center();
    }

    @Override
    public void renderDom(Element cellParent, RenderContext renderContext, UpdateContext updateContext) {
    }

    protected final void commitEditing(Object value) {
        dialog.hide();
        editManager.commitEditing(value);
    }

    protected final void cancelEditing() {
        dialog.hide();
        editManager.cancelEditing();
    }

    protected abstract Widget createComponent(Element parent, Object oldValue);
}
