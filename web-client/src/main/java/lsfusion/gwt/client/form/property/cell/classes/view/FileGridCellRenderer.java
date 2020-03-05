package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.Callback;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.FileBasedGridCellRenderer;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.GwtClientUtils.getModuleImagePath;

public class FileGridCellRenderer extends FileBasedGridCellRenderer {
    public static final String ICON_EMPTY = "empty.png";
    private static final String ICON_FILE = "file.png";


    public FileGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public void renderStatic(Element element, GFont font, boolean isSingle) {
        super.renderStatic(element, font, isSingle);

        element.getStyle().setHeight(100, Style.Unit.PCT);
        element.getStyle().setPosition(Style.Position.RELATIVE);

//        ImageElement image = Document.get().createImageElement();
//        cellElement.appendChild(image);
//        image.getStyle().setVerticalAlign(Style.VerticalAlign.TEXT_BOTTOM);
//        setImageSrc(image, value);
    }

    @Override
    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        Element childElement = element.getFirstChildElement();
        boolean hadImage = childElement != null && "IMG".equals(childElement.getTagName());

        if (value == null && property.isEditableNotNull()) {
            if (childElement == null || hadImage) {
                element.removeAllChildren();

                setBasedEmptyElement(element.appendChild(Document.get().createDivElement()));
            }
        } else {
            if (hadImage) {
                setFileSrc((ImageElement) childElement, value);
            } else {
                element.removeAllChildren();

                ImageElement img = element.appendChild(Document.get().createImageElement());
                img.getStyle().setVerticalAlign(Style.VerticalAlign.MIDDLE);
                setFileSrc(img, value);
            }
        }
    }

    protected void setFileSrc(ImageElement file, Object value) {
        String imagePath = value == null ? ICON_EMPTY : ICON_FILE;
        String colorThemeImagePath = MainFrame.colorTheme.getImagePath(imagePath);
        GwtClientUtils.ensureImage(colorThemeImagePath, new Callback() {
            @Override
            public void onFailure() {
                file.setSrc(getModuleImagePath(imagePath));
            }

            @Override
            public void onSuccess() {
                file.setSrc(getModuleImagePath(colorThemeImagePath));
            }
        });
    }
}
