package platform.gwt.form.shared.view.grid;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import platform.gwt.cellview.client.cell.AbstractCell;
import platform.gwt.form.client.form.ui.GPropertyTable;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;

import static com.google.gwt.dom.client.BrowserEvents.*;

public class GridEditableCell extends AbstractCell<Object> {

    private final GPropertyTable table;
    private final boolean cellTypeCanChange;

    boolean isEditing = false;

    public GridEditableCell(GPropertyTable table) {
        this(table, false);
    }

    public GridEditableCell(GPropertyTable table, boolean cellTypeCanChange) {
        super(DBLCLICK, KEYDOWN, KEYPRESS, BLUR, CONTEXTMENU);
        this.table = table;
        this.cellTypeCanChange = cellTypeCanChange;
    }

    @Override
    public boolean isEditing(Context context, Element parent, Object value) {
        return isEditing;
    }

    public void setEditing(boolean editing) {
        isEditing = editing;
    }

    @Override
    public void onBrowserEvent(final Context context, final Element parent, final Object value, NativeEvent event) {
        // не используется, т.к. перехватывается GPropertyTable
    }

    @Override
    public String getCellType(Context context) {
        if (cellTypeCanChange) {
            GPropertyDraw property = table.getProperty(context);
            return property == null ? null : property.sID;
        }
        return null;
    }

    @Override
    public void renderDom(Context context, DivElement cellElement, Object value) {
        if (isEditing) {
            return;
        }

        GPropertyDraw property = table.getProperty(context);
        if (property != null) {
            GridCellRenderer cellRenderer = property.getGridCellRenderer();
            cellRenderer.renderDom(context, cellElement, value);
        }
    }

    @Override
    public void updateDom(Context context, DivElement cellElement, Object value) {
        if (isEditing) {
            return;
        }

        GPropertyDraw property = table.getProperty(context);
        if (property != null) {
            GridCellRenderer cellRenderer = property.getGridCellRenderer();
            cellRenderer.updateDom(cellElement, context, value);
        }
    }
}