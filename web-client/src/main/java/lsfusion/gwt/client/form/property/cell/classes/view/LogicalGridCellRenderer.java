package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

public class LogicalGridCellRenderer extends AbstractGridCellRenderer {
    private GPropertyDraw property;

    public LogicalGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderDom(DataGrid table, DivElement cellElement, Object value) {
        renderDom(cellElement, value);
        if (table instanceof GGridPropertyTable) {
            cellElement.getStyle().setPosition(Style.Position.RELATIVE);
        }
    }

    @Override
    public void renderDom(Element cellElement, Object value) {
        boolean checked = value != null && (Boolean) value;

        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            cellElement.setAttribute("align", textAlignStyle.getCssName());
        }

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
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Object value) {
        updateDom(cellElement, value);
    }

    @Override
    public void updateDom(Element cellElement, Object value) {
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
