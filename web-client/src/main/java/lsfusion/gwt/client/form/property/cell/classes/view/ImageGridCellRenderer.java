package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.classes.data.GImageType;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedGridCellRenderer;

import static lsfusion.gwt.client.form.property.cell.classes.view.FileGridCellRenderer.ICON_EMPTY;

public class ImageGridCellRenderer extends FileBasedGridCellRenderer {
    protected GPropertyDraw property;

    public ImageGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public void renderStatic(Element element, GFont font, boolean isSingle) {
        super.renderStatic(element, font, isSingle);
        element.removeAllChildren();
        element.setInnerText(null);

        if (isSingle) element.getStyle().setPosition(Style.Position.RELATIVE);
    }

    @Override
    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        if (value == null && property.isEditableNotNull()) {
            setBasedEmptyElement(element);
        } else {
            element.getStyle().clearPadding();
            element.removeClassName("requiredValueString");
            element.setInnerText(null);
            element.setTitle("");

            ImageElement img = element.appendChild(Document.get().createImageElement());

            Style imgStyle = img.getStyle();
            imgStyle.setVerticalAlign(Style.VerticalAlign.MIDDLE);
            imgStyle.setProperty("maxWidth", "100%");
            imgStyle.setProperty("maxHeight", "100%");

            setFileSrc(img, value);
        }
    }

    @Override
    protected void setFileSrc(ImageElement img, Object value) {
        if (value instanceof String && !value.equals("null")) {
            img.setSrc(GwtClientUtils.getDownloadURL((String) value, null, ((GImageType) property.baseType).extension, false)); // form file
        } else {
            img.setSrc(GwtClientUtils.getModuleImagePath(ICON_EMPTY));
        }
    }
}
