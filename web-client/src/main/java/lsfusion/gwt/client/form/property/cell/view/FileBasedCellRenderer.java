package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.HasAutoHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

public abstract class FileBasedCellRenderer extends CellRenderer {
    @Override
    public boolean renderContent(Element element, RenderContext renderContext) {
        return false;
    }

    @Override
    public boolean updateContent(Element element, PValue value, Object extraValue, UpdateContext updateContext) {
        element.setInnerText(null); // remove all

        Element img = null;
        if (value == null && property.isEditableNotNull()) {
            setBasedEmptyElement(element);
        } else {
            element.getStyle().clearPadding();
            element.removeClassName("text-based-value-required");
            element.setTitle("");

            img = (value != null ? getBaseImage(value) : StaticImage.EMPTY).createImage();

            if(property.hasEditObjectAction && value != null) {
                img.addClassName("selectedFileCellHasEdit");
            } else {
                img.removeClassName("selectedFileCellHasEdit");
            }
        }

        Element dragDropLabel = getDragDropLabel();
        element.appendChild(dragDropLabel);
        GwtClientUtils.setupFillParent(dragDropLabel);

        element.appendChild(img);

        return true;
    }

    private Element getDragDropLabel() {
        Label dropFilesLabel = new Label();
        dropFilesLabel.getElement().addClassName("drag-drop-label");
//        dropFilesLabel.setAutoHorizontalAlignment(HasAutoHorizontalAlignment.ALIGN_CENTER);
        DataGrid.initSinkDragDropEvents(dropFilesLabel);
        return dropFilesLabel.getElement();
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        element.getStyle().clearPadding();
        element.removeClassName("text-based-value-required");
        element.setTitle("");

        GwtClientUtils.clearFillParentElement(element);

        return false;
    }

    protected void setBasedEmptyElement(Element element) {
        element.getStyle().setPaddingRight(4, Style.Unit.PX);
        element.getStyle().setPaddingLeft(4, Style.Unit.PX);
        element.setTitle(REQUIRED_VALUE);
        element.addClassName("text-based-value-required");
    }

    protected abstract BaseImage getBaseImage(PValue value);

    protected FileBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    public String format(PValue value, RendererType rendererType, String pattern) {
        return value == null ? "" : value.toString();
    }
}
