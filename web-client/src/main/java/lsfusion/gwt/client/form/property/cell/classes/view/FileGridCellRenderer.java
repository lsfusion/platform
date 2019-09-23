package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

public class FileGridCellRenderer extends AbstractGridCellRenderer {
    public static final String ICON_EMPTY = "static/images/empty.png";
    private static final String ICON_FILE = "static/images/file.png";
    private GPropertyDraw property;

    public FileGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        cellElement.setAttribute("align", "center");
        cellElement.getStyle().setHeight(100, Style.Unit.PCT);
        cellElement.getStyle().setPosition(Style.Position.RELATIVE);
        updateDom(cellElement, table, context, value);

//        ImageElement image = Document.get().createImageElement();
//        cellElement.appendChild(image);
//        image.getStyle().setVerticalAlign(Style.VerticalAlign.TEXT_BOTTOM);
//        setImageSrc(image, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {
        Element childElement = cellElement.getFirstChildElement();
        boolean hadImage = childElement != null && "IMG".equals(childElement.getTagName());

        if (value == null && property.isEditableNotNull()) {
            if (childElement == null || hadImage) {
                cellElement.removeAllChildren();

                DivElement innerElement = cellElement.appendChild(Document.get().createDivElement());
                innerElement.setInnerText(REQUIRED_VALUE);
                innerElement.setTitle(REQUIRED_VALUE);
                innerElement.addClassName("requiredValueString");
            }
        } else {
            if (hadImage) {
                setImageSrc((ImageElement) childElement, value);
            } else {
                cellElement.removeAllChildren();
                
                ImageElement image = cellElement.appendChild(Document.get().createImageElement());
                image.getStyle().setVerticalAlign(Style.VerticalAlign.MIDDLE);
                setImageSrc(image, value);
            }
        }
    }

    private void setImageSrc(ImageElement image, Object value) {
        String imagePath = value == null ? ICON_EMPTY : ICON_FILE;
        image.setSrc(GWT.getModuleBaseURL() + imagePath);
    }
}
