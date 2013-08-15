package lsfusion.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.shared.view.GPropertyDraw;

public class ActionGridCellRenderer extends AbstractGridCellRenderer {
    public ActionGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    private GPropertyDraw property;

    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        divStyle.setBackgroundColor("#F1F1F1");
        divStyle.setBorderColor("#BBB #BBB #A0A0A0");
        divStyle.setBorderWidth(1, Style.Unit.PX);
        divStyle.setBorderStyle(Style.BorderStyle.SOLID);
        divStyle.setProperty("borderRadius", 3, Style.Unit.PX);
        divStyle.setProperty("display", "table");
        divStyle.setWidth(100, Style.Unit.PCT);

        DivElement innerDiv = cellElement.appendChild(Document.get().createDivElement());
        innerDiv.setAttribute("align", "center");
        // избавляемся от двух пикселов, добавляемых к 100%-й высоте рамкой
        cellElement.addClassName("boxSized");
        innerDiv.getStyle().setMarginBottom(-2, Style.Unit.PX);

        if (property.icon != null) {
            ImageElement img = innerDiv.appendChild(Document.get().createImageElement());
            setImage(img, value);
        } else {
            innerDiv.setInnerText("...");
        }
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        if (property.icon != null) {
            ImageElement img = cellElement
                    .getFirstChild()
                    .getFirstChild().cast();
            setImage(img, value);
        }
    }

    private void setImage(ImageElement img, Object value) {
        boolean disabled = value == null || !(Boolean) value;
        String iconPath = property.getIconPath(!disabled);
        img.setSrc(GwtClientUtils.getWebAppBaseURL() + iconPath);

        int height = property.icon.height;
        if (height != -1) {
            img.setHeight(height);
            img.getStyle().setMarginBottom(height > 12 ? -2 : height > 10 ? -1 : 0, Style.Unit.PX);
        }
        if (property.icon.width != -1) {
            img.setWidth(property.icon.width);
        }
    }
}
