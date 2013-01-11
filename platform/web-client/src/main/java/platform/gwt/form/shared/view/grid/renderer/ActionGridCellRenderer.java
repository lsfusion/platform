package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.GPropertyDraw;

public class ActionGridCellRenderer extends AbstractGridCellRenderer {
    public ActionGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    private GPropertyDraw property;

    @Override
    public void renderDom(Cell.Context context, DivElement cellElement, Object value) {
        DivElement div = cellElement.appendChild(Document.get().createDivElement());
        Style divStyle = div.getStyle();
        divStyle.setBackgroundColor("#F1F1F1");
        divStyle.setBorderColor("#BBB #BBB #A0A0A0");
        divStyle.setBorderWidth(1, Style.Unit.PX);
        divStyle.setBorderStyle(Style.BorderStyle.SOLID);
        divStyle.setProperty("borderRadius", 3, Style.Unit.PX);
        divStyle.setProperty("display", "table");
        divStyle.setWidth(100, Style.Unit.PCT);

        DivElement innerDiv = div.appendChild(Document.get().createDivElement());
        innerDiv.setAttribute("align", "center");
        // здесь...
        div.addClassName("boxSized");
        innerDiv.getStyle().setMarginBottom(-2, Style.Unit.PX);

        if (property.iconPath != null) {
            ImageElement img = innerDiv.appendChild(Document.get().createImageElement());
            img.getStyle().setWidth(14, Style.Unit.PX);
            img.getStyle().setHeight(14, Style.Unit.PX);
            setImage(img, value);
            // ... и здесь избавляемся от двух пикселов, добавляемых к 100%-й высоте рамкой
            img.getStyle().setMarginBottom(-2, Style.Unit.PX);
        } else {
            innerDiv.setInnerText("...");
        }
    }

    @Override
    public void updateDom(DivElement cellElement, Cell.Context context, Object value) {
        if (property.iconPath != null) {
            ImageElement img = cellElement
                    .getFirstChild()
                    .getFirstChild()
                    .getFirstChild().cast();
            setImage(img, value);
        }
    }

    private void setImage(ImageElement img, Object value) {
        boolean disabled = value == null || !(Boolean) value;
        String iconPath = property.getIconPath(disabled);
        img.setSrc(GWT.getModuleBaseURL() + "images/" + iconPath);
    }
}
