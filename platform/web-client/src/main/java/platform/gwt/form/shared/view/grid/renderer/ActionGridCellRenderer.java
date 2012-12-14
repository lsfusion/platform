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
        DivElement center = cellElement.appendChild(Document.get().createDivElement());
        center.setAttribute("align", "center");

        DivElement div = center.appendChild(Document.get().createDivElement());
        Style divStyle = div.getStyle();
        divStyle.setBackgroundColor("#F1F1F1");
        divStyle.setBorderColor("#BBB");
        divStyle.setBorderWidth(1, Style.Unit.PX);
        divStyle.setBorderStyle(Style.BorderStyle.SOLID);
        divStyle.setProperty("borderBottom", "1px solid #A0A0A0");
        divStyle.setProperty("borderRadius", 3, Style.Unit.PX);
        divStyle.setWidth(48, Style.Unit.PX);
        divStyle.setHeight(14, Style.Unit.PX);

        if (property.iconPath != null) {
            ImageElement img = div.appendChild(Document.get().createImageElement());
            img.getStyle().setWidth(14, Style.Unit.PX);
            img.getStyle().setHeight(14, Style.Unit.PX);
            setImage(img, value);
        } else {
            DivElement textDiv = div.appendChild(Document.get().createDivElement());
            textDiv.setAttribute("align", "center");
            textDiv.setInnerText("...");
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
