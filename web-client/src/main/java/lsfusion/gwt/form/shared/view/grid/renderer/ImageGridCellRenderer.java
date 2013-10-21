package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.cellview.client.DataGrid;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.client.form.ui.GGridPropertyTable;

public class ImageGridCellRenderer extends AbstractGridCellRenderer {
    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
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

        if (table instanceof GGridPropertyTable) {
            cellElement.getStyle().setPosition(Style.Position.RELATIVE);
        }

        setImageSrc(img, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {
        ImageElement img = cellElement.getFirstChild().cast();
        setImageSrc(img, value);
    }

    private void setImageSrc(ImageElement img, Object value) {
        if (value instanceof String) {
            img.setSrc(imageSrc(value));
        } else {
            img.setSrc(GWT.getModuleBaseURL() + "images/empty.png");
        }
    }

    private String imageSrc(Object value) {
        return GwtClientUtils.getWebAppBaseURL() + "propertyImage?sid=" + value;
    }
}
