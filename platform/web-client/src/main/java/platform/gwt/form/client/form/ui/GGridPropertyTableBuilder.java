package platform.gwt.form.client.form.ui;

public abstract class GGridPropertyTableBuilder<T extends GridDataRecord> extends GPropertyTableBuilder<T> {

    public GGridPropertyTableBuilder(GGridPropertyTable table) {
        super(table);
    }

    public String getBackground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getBackground(column);
    }

    public String getForeground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getForeground(column);
    }
}

