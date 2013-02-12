package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.client.form.ui.dialog.GResizableModalWindow;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.EditManager;

public abstract class DialogBasedGridCellEditor extends AbstractGridCellEditor {
    protected final EditManager editManager;
    protected final GPropertyDraw property;
    private GResizableModalWindow dialog;

    private int width;
    private int height;

    public DialogBasedGridCellEditor(EditManager editManager, GPropertyDraw property, String title) {
        this(editManager, property, title, -1, -1);
    }

    public DialogBasedGridCellEditor(EditManager editManager, GPropertyDraw property, String title, int width, int height) {
        this.editManager = editManager;
        this.property = property;
        this.width = width;
        this.height = height;
        dialog = new GResizableModalWindow(title);
    }

    @Override
    public void onBrowserEvent(Cell.Context context, Element parent, Object value, NativeEvent event) {
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        dialog.setContentWidget(createComponent());
        if (width != -1 && height != -1) {
            dialog.center();
            dialog.setContentSize(width, height);
        }
        dialog.center();
    }

    @Override
    public void renderDom(Cell.Context context, DivElement cellParent, Object value) {
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

    protected abstract Widget createComponent();
}
