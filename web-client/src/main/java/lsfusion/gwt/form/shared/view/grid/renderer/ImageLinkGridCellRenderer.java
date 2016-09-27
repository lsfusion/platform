package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.form.ui.GGridPropertyTable;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

public class ImageLinkGridCellRenderer extends AbstractGridCellRenderer {
    protected GPropertyDraw property;

    public ImageLinkGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        if (table instanceof GGridPropertyTable) {
            cellElement.getStyle().setPosition(Style.Position.RELATIVE);
        }
        
        updateDom(cellElement, table, context, value);
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

            ImageElement img = cellElement.appendChild(Document.get().createImageElement());
            
            Style imgStyle = img.getStyle();
            imgStyle.setVerticalAlign(Style.VerticalAlign.MIDDLE);
            imgStyle.setProperty("maxWidth", "100%");
            imgStyle.setProperty("maxHeight", "100%");
            imgStyle.setProperty("margin", "auto");

            imgStyle.setPosition(Style.Position.ABSOLUTE);
            imgStyle.setTop(0, Style.Unit.PX);
            imgStyle.setLeft(0, Style.Unit.PX);
            imgStyle.setBottom(0, Style.Unit.PX);
            imgStyle.setRight(0, Style.Unit.PX);

            setImageSrc(img, value);
        }
    }

    private void setImageSrc(ImageElement img, Object value) {
        if (value instanceof String) {
            img.setSrc((String) value);
        } else {
            img.setSrc(GWT.getModuleBaseURL() + "images/empty.png");
        }
    }
}