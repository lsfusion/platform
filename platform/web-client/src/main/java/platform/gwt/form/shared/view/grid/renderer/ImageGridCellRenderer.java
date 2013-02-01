package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import platform.gwt.cellview.client.cell.Cell;

public class ImageGridCellRenderer extends AbstractGridCellRenderer {
    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        cellElement.setAttribute("align", "center");

        ImageElement img = cellElement.appendChild(Document.get().createImageElement());
        img.setSrc(imageSrc(value));
        img.getStyle().setVerticalAlign(Style.VerticalAlign.MIDDLE);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        ImageElement img = cellElement.getFirstChild().cast();
        img.setSrc(imageSrc(value));
    }

    private String imageSrc(Object value) {
        return GWT.getHostPageBaseURL() + "propertyImage?sid=" + value;
    }
}
