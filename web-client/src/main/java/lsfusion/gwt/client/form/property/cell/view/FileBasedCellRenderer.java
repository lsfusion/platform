package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.StaticImage;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;

import static lsfusion.gwt.client.view.MainFrame.v5;

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
            GwtClientUtils.removeClassName(element, "text-based-value-required");
            element.setTitle("");

            img = (value != null ? getBaseImage(value) : StaticImage.EMPTY).createImage();

            if(property.hasEditObjectAction && value != null) {
                GwtClientUtils.addClassName(img, "selected-file-cell-has-edit", "selectedFileCellHasEdit", v5);
            } else {
                GwtClientUtils.removeClassName(img, "selected-file-cell-has-edit", "selectedFileCellHasEdit", v5);
            }
        }

        Element dragDropLabel = getDragDropLabel(img);
        element.appendChild(dragDropLabel);
        GwtClientUtils.setupFillParent(dragDropLabel);

        if(img != null) {
            element.appendChild(img);
        }

        return true;
    }

    private Element getDragDropLabel(Element img) {
        Label dropFilesLabel = new Label();
        GwtClientUtils.addClassName(dropFilesLabel.getElement(), "drag-drop-label");
        if(img == null) {
            dropFilesLabel.setText(REQUIRED_VALUE);
        }
//        dropFilesLabel.setAutoHorizontalAlignment(HasAutoHorizontalAlignment.ALIGN_CENTER);
        DataGrid.initSinkDragDropEvents(dropFilesLabel);
        return dropFilesLabel.getElement();
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        element.getStyle().clearPadding();
        GwtClientUtils.removeClassName(element, "text-based-value-required");
        element.setTitle("");

        GwtClientUtils.clearFillParentElement(element);

        return false;
    }

    protected void setBasedEmptyElement(Element element) {
        element.getStyle().setPaddingRight(4, Style.Unit.PX);
        element.getStyle().setPaddingLeft(4, Style.Unit.PX);
        element.setTitle(REQUIRED_VALUE);
        GwtClientUtils.addClassName(element, "text-based-value-required");
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
