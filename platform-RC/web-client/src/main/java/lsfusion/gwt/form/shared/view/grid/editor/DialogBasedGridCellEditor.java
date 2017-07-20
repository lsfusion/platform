package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ui.WindowBox;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public abstract class DialogBasedGridCellEditor extends AbstractGridCellEditor {
    protected final EditManager editManager;
    protected final GPropertyDraw property;

    protected final WindowBox dialog;

    protected final int width;
    protected final int height;

    public DialogBasedGridCellEditor(EditManager editManager, GPropertyDraw property, String title) {
        this(editManager, property, title, -1, -1);
    }

    public DialogBasedGridCellEditor(EditManager editManager, GPropertyDraw property, String title, int width, int height) {
        this.editManager = editManager;
        this.property = property;
        this.width = width;
        this.height = height;
        
        dialog = new WindowBox(false, true, false, true, true) {
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
    public void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event) {
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        Widget content = createComponent(editEvent, context, parent, oldValue);
        if (width != -1 && height != -1) {
            content.setPixelSize(width, height);
        }
        dialog.setWidget(content);
        dialog.center();
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellParent, Object value) {
    }

    @Override
    public boolean replaceCellRenderer() {
        return false;
    }

    protected final void commitEditing(Object value) {
        dialog.hide();
        editManager.commitEditing(value);
    }

    protected final void cancelEditing() {
        dialog.hide();
        editManager.cancelEditing();
    }

    protected abstract Widget createComponent(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue);
}
