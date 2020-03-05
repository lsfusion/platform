package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

public class ActionGridCellRenderer extends AbstractGridCellRenderer {
    public ActionGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    private GPropertyDraw property;

    @Override
    public void renderStatic(Element element, GFont font, boolean isSingle) {
        Style divStyle = element.getStyle();
        element.addClassName("gwt-Button");
        divStyle.setWidth(100, Style.Unit.PCT);
        divStyle.setPadding(0, Style.Unit.PX);

        // избавляемся от двух пикселов, добавляемых к 100%-й высоте рамкой
        element.addClassName("boxSized");
    }

    @Override
    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        DivElement innerTop = element.appendChild(Document.get().createDivElement());
        innerTop.getStyle().setHeight(50, Style.Unit.PCT);
        innerTop.getStyle().setPosition(Style.Position.RELATIVE);
        innerTop.setAttribute("align", "center");

        DivElement innerBottom = element.appendChild(Document.get().createDivElement());
        innerBottom.getStyle().setHeight(50, Style.Unit.PCT);

        if (property.getImage() != null) {
            ImageElement img = innerTop.appendChild(Document.get().createImageElement());
            img.getStyle().setPosition(Style.Position.ABSOLUTE);
            img.getStyle().setLeft(50, Style.Unit.PCT);
            setImage(img, value);
        } else {
            LabelElement label = element.getFirstChild().getFirstChild().cast();
            if (property.font == null && isSingle) {
                property.font = font;
            }
            if (property.font != null) {
                property.font.apply(label.getStyle());
            }
            label.setInnerText("...");
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
