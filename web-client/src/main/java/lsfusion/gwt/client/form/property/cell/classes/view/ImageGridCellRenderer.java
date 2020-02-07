package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.classes.data.GImageType;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

import static lsfusion.gwt.client.form.property.cell.classes.view.FileGridCellRenderer.ICON_EMPTY;

public class ImageGridCellRenderer extends AbstractGridCellRenderer {
    protected GPropertyDraw property;
    
    public ImageGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        if (table instanceof GGridPropertyTable) {
            cellElement.getStyle().setPosition(Style.Position.RELATIVE);
        }
        cellElement.getStyle().setWhiteSpace(Style.WhiteSpace.PRE);
        
        updateDom(cellElement, table, context, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {     
        cellElement.removeAllChildren();
        cellElement.setInnerText(null);
        
        if (value == null && property.isEditableNotNull()) {
            cellElement.getStyle().setPaddingRight(4, Style.Unit.PX);
            cellElement.getStyle().setPaddingLeft(4, Style.Unit.PX);
            cellElement.setInnerText(REQUIRED_VALUE);
            cellElement.setTitle(REQUIRED_VALUE);
            cellElement.addClassName("requiredValueString");
        } else {
            cellElement.getStyle().clearPadding();
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

    protected void setImageSrc(ImageElement img, Object value) {
        if (value instanceof String && !value.equals("null")) {
            img.setSrc(GwtClientUtils.getDownloadURL((String) value, null, ((GImageType)property.baseType).extension, false)); // form file
        } else {
            img.setSrc(GWT.getModuleBaseURL() + ICON_EMPTY);
        }
    }

}
