package lsfusion.gwt.client.form.object.table.view;

import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

public class GGridPropertyTableBuilder<T extends GridDataRecord> extends GPropertyTableBuilder<T> {

    public GGridPropertyTableBuilder(GGridPropertyTable table) {
        super(table);
    }

    public String getBackground(GridDataRecord rowValue, int row, int column) {
        return ((GGridPropertyTable)cellTable).getCellBackground(rowValue, row, column);
    }

    public String getForeground(GridDataRecord rowValue, int row, int column) {
        return ((GGridPropertyTable)cellTable).getCellForeground(rowValue, row, column);
    }
}

