package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.form.ui.GGridPropertyTable;

public class LogicalGridCellRenderer extends AbstractGridCellRenderer {

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        boolean checked = value != null && (Boolean) value;

        cellElement.setAttribute("align", "center");

        ImageElement img = cellElement.appendChild(Document.get().createImageElement());
        img.setSrc(getCBImagePath(checked));

        Style imgStyle = img.getStyle();
        imgStyle.setVerticalAlign(Style.VerticalAlign.MIDDLE);
        imgStyle.setProperty("margin", "auto");

        imgStyle.setPosition(Style.Position.ABSOLUTE);
        imgStyle.setTop(0, Style.Unit.PX);
        imgStyle.setLeft(0, Style.Unit.PX);
        imgStyle.setBottom(0, Style.Unit.PX);
        imgStyle.setRight(0, Style.Unit.PX);

        if (table instanceof GGridPropertyTable) {
            cellElement.getStyle().setPosition(Style.Position.RELATIVE);
        }
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {
        ImageElement img = cellElement
                .getFirstChild().cast();
        img.setSrc(getCBImagePath(value));
    }

    private String getCBImagePath(Object value) {
        boolean checked = value != null && (Boolean) value;
        return GWT.getModuleBaseURL() + "images/checkbox_" + (checked ? "checked" : "unchecked") + ".png";
    }
}
