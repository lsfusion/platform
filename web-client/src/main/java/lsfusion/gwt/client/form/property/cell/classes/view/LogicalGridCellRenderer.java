package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

public class LogicalGridCellRenderer extends AbstractGridCellRenderer {

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        boolean checked = value != null && (Boolean) value;

        Style checkStyle;
        // logical class is rendered as checkbox input to make all checkboxes look the same. 
        // in case of renderer we want to prevent checkbox from listening to mouse events.
        // for this purpose we use property "pointerEvents: none", which doesn't work in IE.
        if (GwtClientUtils.isIEUserAgent()) {
            ImageElement img = cellElement.appendChild(Document.get().createImageElement());
            img.setSrc(getCBImagePath(checked));

            checkStyle = img.getStyle();
            checkStyle.setVerticalAlign(Style.VerticalAlign.MIDDLE);
        } else {
            InputElement input = cellElement.appendChild(Document.get().createCheckInputElement());
            input.setTabIndex(-1);
            input.setChecked(checked);

            checkStyle = input.getStyle();
            checkStyle.setProperty("pointerEvents", "none");
        }
        
        checkStyle.setProperty("margin", "auto");
        checkStyle.setPosition(Style.Position.ABSOLUTE);
        checkStyle.setTop(0, Style.Unit.PX);
        checkStyle.setLeft(0, Style.Unit.PX);
        checkStyle.setBottom(0, Style.Unit.PX);
        checkStyle.setRight(0, Style.Unit.PX);

        if (table instanceof GGridPropertyTable) {
            cellElement.getStyle().setPosition(Style.Position.RELATIVE);
        }
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {
        if (GwtClientUtils.isIEUserAgent()) {
            ImageElement img = cellElement.getFirstChild().cast();
            img.setSrc(getCBImagePath(value));
        } else {
            InputElement input = cellElement.getFirstChild().cast();
            input.setChecked(value != null && (Boolean) value);
        }
    }

    private String getCBImagePath(Object value) {
        boolean checked = value != null && (Boolean) value;
        return GwtClientUtils.getModuleImagePath("checkbox_" + (checked ? "checked" : "unchecked") + ".png");
    }
}
