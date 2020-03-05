package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ImageDescription;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public class ActionGridCellRenderer extends TextBasedGridCellRenderer {
    public ActionGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public void renderStatic(Element element, GFont font, boolean isSingle) {
        Style style = element.getStyle();
        element.addClassName("gwt-Button");
        style.setWidth(100, Style.Unit.PCT);
        style.setPadding(0, Style.Unit.PX);

        DivElement innerTop = element.appendChild(Document.get().createDivElement());
        innerTop.getStyle().setHeight(50, Style.Unit.PCT);
        innerTop.getStyle().setPosition(Style.Position.RELATIVE);
        innerTop.setAttribute("align", "center");

        element.appendChild(Document.get().createDivElement()).getStyle().setHeight(50, Style.Unit.PCT);
        // избавляемся от двух пикселов, добавляемых к 100%-й высоте рамкой
        element.addClassName("boxSized");
    }

    @Override
    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        if (property.getImage() != null) {
            ImageElement img = element.getFirstChildElement().appendChild(Document.get().createImageElement());
            img.getStyle().setPosition(Style.Position.ABSOLUTE);
            img.getStyle().setLeft(50, Style.Unit.PCT);
            setImage(img, value);
        } else {
            LabelElement label = element.getFirstChild().getFirstChild().cast();
            setBasedTextFonts(label.getStyle(), font, isSingle);
            setInnerText(label, "...");
        }
    }

    @Override
    protected void setInnerText(Element element, String innerText) {
        element.setInnerText(innerText);
    }

    @Override
    protected String castToString(Object value) {
        return null;
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
