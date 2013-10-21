package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;

public class FileGridCellRenderer extends AbstractGridCellRenderer {
    private final String ICON_EMPTY = "empty.png";
    private final String ICON_FILE = "file.png";

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        cellElement.setAttribute("align", "center");

        ImageElement image = Document.get().createImageElement();
        cellElement.appendChild(image);
        image.getStyle().setVerticalAlign(Style.VerticalAlign.TEXT_BOTTOM);
        setImageSrc(image, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {
        ImageElement image = cellElement.getFirstChild().cast();
        setImageSrc(image, value);
    }

    private void setImageSrc(ImageElement image, Object value) {
        String imagePath = value == null ? ICON_EMPTY : ICON_FILE;
        image.setSrc(GWT.getModuleBaseURL() + "images/" + imagePath);
    }
}
