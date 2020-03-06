package lsfusion.gwt.client.form.property.cell.classes.view;

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
    public FileGridCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public void renderStatic(Element element, GFont font, boolean isSingle) {
        super.renderStatic(element, font, isSingle);
        element.getStyle().setHeight(100, Style.Unit.PCT);
    }

    @Override
    protected String prepareFilePath(Object value) {
        ImageElement file = ImageElement.createObject().cast();

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
        return file.getSrc();
    }

    private void setImageSrc(ImageElement image, Object value) {
        GwtClientUtils.setThemeImage(value == null ? ICON_EMPTY : ICON_FILE, image::setSrc);
    }
}
