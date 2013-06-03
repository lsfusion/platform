package lsfusion.gwt.form.client.form.ui;

public class GSinglePropertyTableBuilder extends GPropertyTableBuilder<Object> {

    private GSinglePropertyTable table;

    public GSinglePropertyTableBuilder(GSinglePropertyTable table) {
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

