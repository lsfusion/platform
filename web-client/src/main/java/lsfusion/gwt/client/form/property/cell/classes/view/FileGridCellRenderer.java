package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.*;
import lsfusion.gwt.client.base.Callback;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.GwtClientUtils.getModuleImagePath;

public class FileGridCellRenderer extends AbstractGridCellRenderer {
    public static final String ICON_EMPTY = "empty.png";
    private static final String ICON_FILE = "file.png";
    private GPropertyDraw property;

    public FileGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    @Override
    public void renderDom(DataGrid table, DivElement cellElement, Object value) {
        renderDom(cellElement, value);
    }

    @Override
    public void renderDom(Element cellElement, Object value) {
        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            cellElement.setAttribute("align", textAlignStyle.getCssName());
        }
        cellElement.getStyle().setHeight(100, Style.Unit.PCT);
        cellElement.getStyle().setPosition(Style.Position.RELATIVE);
        cellElement.getStyle().setWhiteSpace(Style.WhiteSpace.PRE);
        updateDom(cellElement, value);

//        ImageElement image = Document.get().createImageElement();
//        cellElement.appendChild(image);
//        image.getStyle().setVerticalAlign(Style.VerticalAlign.TEXT_BOTTOM);
//        setImageSrc(image, value);
    }

    @Override
    public void updateDom(DivElement cellElement, DataGrid table, Object value) {
        updateDom(cellElement, value);
    }

    @Override
    public void updateDom(Element cellElement, Object value) {
        Element childElement = cellElement.getFirstChildElement();
        boolean hadImage = childElement != null && "IMG".equals(childElement.getTagName());

        if (value == null && property.isEditableNotNull()) {
            if (childElement == null || hadImage) {
                cellElement.removeAllChildren();

                DivElement innerElement = cellElement.appendChild(Document.get().createDivElement());
                innerElement.getStyle().setPaddingRight(4, Style.Unit.PX);
                innerElement.getStyle().setPaddingLeft(4, Style.Unit.PX);
                innerElement.setInnerText(REQUIRED_VALUE);
                innerElement.setTitle(REQUIRED_VALUE);
                innerElement.addClassName("requiredValueString");
            }
        } else {
            if (hadImage) {
                setImageSrc((ImageElement) childElement, value);
            } else {
                cellElement.removeAllChildren();

                ImageElement image = cellElement.appendChild(Document.get().createImageElement());
                image.getStyle().setVerticalAlign(Style.VerticalAlign.MIDDLE);
                setImageSrc(image, value);
            }
        }
    }

    private void setImageSrc(ImageElement image, Object value) {
        String imagePath = value == null ? ICON_EMPTY : ICON_FILE;
        String colorThemeImagePath = MainFrame.colorTheme.getImagePath(imagePath);
        GwtClientUtils.ensureImage(colorThemeImagePath, new Callback() {
            @Override
            public void onFailure() {
                image.setSrc(getModuleImagePath(imagePath));
            }

            @Override
            public void onSuccess() {
                image.setSrc(getModuleImagePath(colorThemeImagePath));
            }
        });
    }
}
