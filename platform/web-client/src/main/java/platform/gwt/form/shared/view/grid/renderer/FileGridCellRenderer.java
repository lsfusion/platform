package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import platform.gwt.cellview.client.cell.Cell;

public class FileGridCellRenderer extends AbstractGridCellRenderer {
    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        cellElement.setAttribute("align", "center");

        ImageElement image = Document.get().createImageElement();
        cellElement.appendChild(image);
        image.getStyle().setVerticalAlign(Style.VerticalAlign.TEXT_BOTTOM);
        setImageSrc(image, value);
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        ImageElement image = cellElement.getFirstChild().cast();
        setImageSrc(image, value);
    }

    private void setImageSrc(ImageElement image, Object value) {
        image.setSrc(value != null ? GWT.getModuleBaseURL() + "images/file.png" : GWT.getHostPageBaseURL() + "images/empty.png");
    }
}
