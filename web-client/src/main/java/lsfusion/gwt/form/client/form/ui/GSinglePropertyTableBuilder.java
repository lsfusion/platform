package lsfusion.gwt.form.client.form.ui;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.TableCellElement;

public class GSinglePropertyTableBuilder extends GPropertyTableBuilder<Object> {

    private GSinglePropertyTable table;
    private boolean stripCellHeight = false;

    public GSinglePropertyTableBuilder(GSinglePropertyTable table) {
        super(table);
        this.table = table;
    }

    public void setStripCellHeight(boolean stripCellHeight) {
        this.stripCellHeight = stripCellHeight;
    }

    public String getBackground(Object rowValue, int row, int column) {
        return table.getBackground();
    }

    public String getForeground(Object rowValue, int row, int column) {
        return table.getForeground();
    }

    @Override
    protected DivElement createCellInnerDiv() {
        DivElement innerDiv = stripCellHeight
                              ? Document.get().createDivElement()
                              : super.createCellInnerDiv();
        String innerDivStyleName = ((GSinglePropertyTable.GSinglePropertyTableStyle)table.getResources().style()).dataGridCellInnerDiv();
        innerDiv.addClassName(innerDivStyleName);
        return innerDiv;
    }

    @Override
    protected void updateTD(int rowIndex, Object rowValue, TableCellElement td, int columnIndex, boolean updateCellHeight) {
        if (stripCellHeight) {
            super.updateTD(rowIndex, rowValue, td, columnIndex, false);
            td.getStyle().setVerticalAlign(Style.VerticalAlign.TOP);
        } else {
            super.updateTD(rowIndex, rowValue, td, columnIndex, updateCellHeight);
        }
    }
}

