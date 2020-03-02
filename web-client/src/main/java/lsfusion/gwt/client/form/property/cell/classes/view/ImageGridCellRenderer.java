package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
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
    public void renderDom( DataGrid table, DivElement cellElement, Object value) {
        if (table instanceof GGridPropertyTable) {
            cellElement.getStyle().setPosition(Style.Position.RELATIVE);
        }
        cellElement.getStyle().setWhiteSpace(Style.WhiteSpace.PRE);

        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            cellElement.setAttribute("align", textAlignStyle.getCssName());
        }
        
        updateDom(cellElement, table, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Object value) {
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

            setImageSrc(img, value);
        }
    }

    protected void setImageSrc(ImageElement img, Object value) {
        if (value instanceof String && !value.equals("null")) {
            img.setSrc(GwtClientUtils.getDownloadURL((String) value, null, ((GImageType)property.baseType).extension, false)); // form file
        } else {
            img.setSrc(GwtClientUtils.getModuleImagePath(ICON_EMPTY));
        }
    }

}
