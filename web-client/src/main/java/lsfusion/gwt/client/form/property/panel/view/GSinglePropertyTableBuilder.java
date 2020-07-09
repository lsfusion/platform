package lsfusion.gwt.client.form.property.panel.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.form.property.table.view.GPropertyTableBuilder;

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
        String innerDivStyleName = table.getStyle().dataGridCellInnerDiv();
        innerDiv.addClassName(innerDivStyleName);
        return innerDiv;
    }

    @Override
    protected void updateTD(int rowIndex, Object rowValue, TableRowElement tr, TableCellElement td, int columnIndex, boolean updateCellHeight) {
        if (stripCellHeight) {
            super.updateTD(rowIndex, rowValue, tr, td, columnIndex, false);
            td.getStyle().setHeight(100, Style.Unit.PCT);
//            td.getStyle().setVerticalAlign(Style.VerticalAlign.TOP);                            
        } else {
            super.updateTD(rowIndex, rowValue, tr, td, columnIndex, updateCellHeight);
        }
    }
}

