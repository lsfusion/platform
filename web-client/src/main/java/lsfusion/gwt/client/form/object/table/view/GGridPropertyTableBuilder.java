package lsfusion.gwt.client.form.object.table.view;

import com.google.gwt.dom.client.TableRowElement;
import lsfusion.gwt.client.base.view.grid.RowIndexHolder;
import lsfusion.gwt.client.form.object.GGroupObject;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

public class GGridPropertyTableBuilder<T extends GridDataRecord> extends GPropertyTableBuilder<T> {
    private final GGridPropertyTable<T, ?, ?> table;

    public GGridPropertyTableBuilder(GGridPropertyTable<T, ?, ?> table) {
        super(table);
        this.table = table;
    }

    // A ROW'S ADDRESS on its element, for what reads the page - the browser's agent, which names the row to the page's
    // tool by it (WebMCP, GFormController.readRow): the row's group, and its key as a React view has it of its rows
    // (GGroupObjectValue.toKeyString). Said as the row is built and again as it is updated, an element being used for
    // another row; none on a row that is no row of a group (GGridPropertyTable.getRowGroup)
    @Override
    protected void setRowValueIndex(TableRowElement tr, int rowIndex, RowIndexHolder row) {
        super.setRowValueIndex(tr, rowIndex, row);

        T record = (T) row;
        GGroupObject group = table.getRowGroup(record);
        if (group != null) {
            tr.setAttribute("lsfusion-group", group.getSID());
            tr.setAttribute("lsfusion-row", record.getKey().toKeyString());
        } else {
            tr.removeAttribute("lsfusion-group");
            tr.removeAttribute("lsfusion-row");
        }
    }
}
