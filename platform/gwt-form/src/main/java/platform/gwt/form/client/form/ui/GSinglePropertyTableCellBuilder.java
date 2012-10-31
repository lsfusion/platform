package platform.gwt.form.client.form.ui;

public class GSinglePropertyTableCellBuilder extends GPropertyTableCellBuilder<Object> {

    private GSinglePropertyTable table;

    public GSinglePropertyTableCellBuilder(GSinglePropertyTable table) {
        super(table);
        this.table = table;
    }

    public String getBackground(Object rowValue, int row, int column) {
        return table.getBackground();
    }

    public String getForeground(Object rowValue, int row, int column) {
        return table.getForeground();
    }
}

