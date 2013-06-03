package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.cellview.client.cell.Cell;

public class LogicalGridCellRenderer extends AbstractGridCellRenderer {

    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        boolean checked = value != null && (Boolean) value;

        cellElement.setAttribute("align", "center");

        ImageElement img = cellElement.appendChild(Document.get().createImageElement());
        img.setSrc(getCBImagePath(checked));
        img.getStyle().setVerticalAlign(Style.VerticalAlign.TEXT_BOTTOM);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        ImageElement img = cellElement
                .getFirstChild().cast();
        img.setSrc(getCBImagePath(value));
    }

    private String getCBImagePath(Object value) {
        boolean checked = value != null && (Boolean) value;
        return GWT.getModuleBaseURL() + "images/checkbox_" + (checked ? "checked" : "unchecked") + ".png";
    }
}
