package lsfusion.gwt.client.form.object.table.view;

import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

import java.util.Optional;

public class GGridPropertyTableBuilder<T extends GridDataRecord> extends GPropertyTableBuilder<T> {

    public GGridPropertyTableBuilder(GGridPropertyTable table) {
        super(table);
    }

    private String getColumnSID(int column) {
        return ((GGridPropertyTable) cellTable).getColumnSID(column);
    }

    public String getBackground(T rowValue, int row, int column) {
        return rowValue.getBackground(getColumnSID(column));
    }

    public String getForeground(T rowValue, int row, int column) {
        return rowValue.getForeground(getColumnSID(column));
    }

    @Override
    public Optional<Object> getImage(T rowValue, int row, int column) {
        return rowValue.getImage(getColumnSID(column));
    }
}

