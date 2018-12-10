package lsfusion.gwt.client.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.cellview.DataGrid;
import lsfusion.gwt.client.cellview.cell.Cell;
import lsfusion.gwt.shared.view.GPropertyDraw;

public class FileGridCellRenderer extends AbstractGridCellRenderer {
    public static final String ICON_EMPTY = "empty.png";
    private static final String ICON_FILE = "file.png";
    private GPropertyDraw property;

    public FileGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        cellElement.setAttribute("align", "center");
        updateDom(cellElement, table, context, value);

//        ImageElement image = Document.get().createImageElement();
//        cellElement.appendChild(image);
//        image.getStyle().setVerticalAlign(Style.VerticalAlign.TEXT_BOTTOM);
//        setImageSrc(image, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {
        cellElement.removeAllChildren();
        cellElement.setInnerText(null);
        
        if (value == null && property.isEditableNotNull()) {
            cellElement.setInnerText(REQUIRED_VALUE);
            cellElement.setTitle(REQUIRED_VALUE);
            cellElement.addClassName("requiredValueString");    
        } else {
            cellElement.removeClassName("requiredValueString");
            cellElement.setInnerText(null);
            cellElement.setTitle("");

            ImageElement image = Document.get().createImageElement();
            cellElement.appendChild(image);
            image.getStyle().setVerticalAlign(Style.VerticalAlign.TEXT_BOTTOM);
            setImageSrc(image, value);
        }
    }

    private void setImageSrc(ImageElement image, Object value) {
        String imagePath = value == null ? ICON_EMPTY : ICON_FILE;
        image.setSrc(GWT.getModuleBaseURL() + "images/" + imagePath);
    }
}
