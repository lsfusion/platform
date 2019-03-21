package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.base.view.grid.cell.Cell;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;
import lsfusion.gwt.shared.form.design.GFont;
import lsfusion.gwt.shared.form.design.GFontMetrics;
import lsfusion.gwt.shared.form.property.GPropertyDraw;

public class ActionGridCellRenderer extends AbstractGridCellRenderer {
    public ActionGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    private GPropertyDraw property;

    @Override
    public void renderDom(Cell.Context context, DataGrid table, DivElement cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        divStyle.setBackgroundColor("#F1F1F1");
        divStyle.setBorderColor("#BBB #BBB #A0A0A0");
        divStyle.setBorderWidth(1, Style.Unit.PX);
        divStyle.setBorderStyle(Style.BorderStyle.SOLID);
        divStyle.setProperty("borderRadius", 3, Style.Unit.PX);
        divStyle.setWidth(100, Style.Unit.PCT);

        // избавляемся от двух пикселов, добавляемых к 100%-й высоте рамкой
        cellElement.addClassName("boxSized");

        DivElement innerTop = cellElement.appendChild(Document.get().createDivElement());
        innerTop.getStyle().setHeight(50, Style.Unit.PCT);
        innerTop.getStyle().setPosition(Style.Position.RELATIVE);
        innerTop.setAttribute("align", "center");

        DivElement innerBottom = cellElement.appendChild(Document.get().createDivElement());
        innerBottom.getStyle().setHeight(50, Style.Unit.PCT);

        if (property.icon != null) {
            ImageElement img = innerTop.appendChild(Document.get().createImageElement());
            img.getStyle().setPosition(Style.Position.ABSOLUTE);
            img.getStyle().setLeft(50, Style.Unit.PCT);
            setImage(img, value);
        } else {
            LabelElement label = innerTop.appendChild(Document.get().createLabelElement());

            GFont font = property.font;
            if (font == null && table instanceof GGridPropertyTable) {
                font = ((GGridPropertyTable) table).font;
            }
            if (font != null) {
                font.apply(label.getStyle());
            }
            
            label.getStyle().setBottom(-GFontMetrics.getSymbolHeight(font) / 2, Style.Unit.PX);
            
            label.setInnerText("...");
        }
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Cell.Context context, Object value) {
        if (property.icon == null) {
            
            LabelElement label = cellElement.getFirstChild().getFirstChild().cast();
            GFont font = property.font;
            if (font == null && table instanceof GGridPropertyTable) {
                font = ((GGridPropertyTable) table).font;
            }
            if (font != null) {
                font.apply(label.getStyle());
            }   
        }
        
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
            img.getStyle().setBottom(- height / 2, Style.Unit.PX);
        }
        if (property.icon.width != -1) {
            img.setWidth(property.icon.width);
            img.getStyle().setMarginLeft(- property.icon.width / 2, Style.Unit.PX);
        }
    }
}
