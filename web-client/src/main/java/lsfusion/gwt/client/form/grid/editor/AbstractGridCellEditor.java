package lsfusion.gwt.client.form.grid.editor;

import com.google.gwt.dom.client.DivElement;
import lsfusion.gwt.client.cellview.DataGrid;
import lsfusion.gwt.client.cellview.cell.Cell;
import lsfusion.gwt.shared.view.GExtInt;
import lsfusion.gwt.shared.view.GPropertyDraw;
import lsfusion.gwt.shared.view.classes.*;
import lsfusion.gwt.shared.view.classes.link.*;
import lsfusion.gwt.client.form.grid.EditManager;
import lsfusion.gwt.client.form.grid.editor.rich.RichTextGridCellEditor;

import java.util.ArrayList;

public abstract class AbstractGridCellEditor implements GridCellEditor {
    @Override
    public abstract void renderDom(Cell.Context context, DataGrid table, DivElement cellParent, Object value);

    @Override
    public boolean replaceCellRenderer() {
        return true;
    }
}
