package platform.gwt.form2.client.form.ui;

public class GGridPropertyTableCellBuilder extends GPropertyTableCellBuilder<GridDataRecord> {

    public GGridPropertyTableCellBuilder(GGridPropertyTable table) {
        super(table);
    }

    public String getBackground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getBackground(column);
    }

    public String getForeground(GridDataRecord rowValue, int row, int column) {
        return rowValue.getForeground(column);
    }
}

