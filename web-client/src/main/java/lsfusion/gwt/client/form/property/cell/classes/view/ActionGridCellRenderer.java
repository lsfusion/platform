package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.object.table.view.GGridPropertyTable;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

public class ActionGridCellRenderer extends AbstractGridCellRenderer {
    public ActionGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    private GPropertyDraw property;

    @Override
    public void renderDom(DataGrid table, DivElement cellElement, Object value) {
        if (property.getImage() == null) {
            if (property.font == null && table instanceof GGridPropertyTable) {
                property.font = ((GGridPropertyTable) table).font;
            }
        }
    }

    @Override
    public void renderDom(Element cellElement, Object value) {
        Style divStyle = cellElement.getStyle();
        cellElement.addClassName("gwt-Button");
        divStyle.setWidth(100, Style.Unit.PCT);
        divStyle.setPadding(0, Style.Unit.PX);

        // избавляемся от двух пикселов, добавляемых к 100%-й высоте рамкой
        cellElement.addClassName("boxSized");

        DivElement innerTop = cellElement.appendChild(Document.get().createDivElement());
        innerTop.getStyle().setHeight(50, Style.Unit.PCT);
        innerTop.getStyle().setPosition(Style.Position.RELATIVE);
        innerTop.setAttribute("align", "center");

        DivElement innerBottom = cellElement.appendChild(Document.get().createDivElement());
        innerBottom.getStyle().setHeight(50, Style.Unit.PCT);

        if (property.getImage() != null) {
            ImageElement img = innerTop.appendChild(Document.get().createImageElement());
            img.getStyle().setPosition(Style.Position.ABSOLUTE);
            img.getStyle().setLeft(50, Style.Unit.PCT);
            setImage(img, value);
        } else {
            LabelElement label = innerTop.appendChild(Document.get().createLabelElement());
            if (property.font != null) {
                property.font.apply(label.getStyle());
            }
            label.setInnerText("...");
        }
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Object value) {
        if (property.getImage() == null) {
            if (property.font == null && table instanceof GGridPropertyTable) {
                property.font = ((GGridPropertyTable) table).font;
            }
        }
    }

    @Override
    public void updateDom(Element cellElement, Object value) {
        if (property.getImage() == null) {
            LabelElement label = cellElement.getFirstChild().getFirstChild().cast();
            if (property.font != null) {
                property.font.apply(label.getStyle());
            }
        } else {
            setImage(cellElement
                    .getFirstChild()
                    .getFirstChild().cast(), value);
        }
    }

    private void setImage(ImageElement img, Object value) {
        boolean enabled = value != null && (Boolean) value;
        ImageDescription image = property.getImage(enabled);
        if (image != null) {
            img.setSrc(GwtClientUtils.getAppImagePath(image.url));

            int height = image.height;
            if (height != -1) {
                img.setHeight(height);
                img.getStyle().setBottom(-(double) height / 2, Style.Unit.PX);
            }
            if (image.width != -1) {
                img.setWidth(image.width);
                img.getStyle().setMarginLeft(-(double) image.width / 2, Style.Unit.PX);
            }
        }
    }
}
