package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.HasAutoHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.client.base.BaseImage;
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

            Style imgStyle = img.getStyle();
            imgStyle.setVerticalAlign(Style.VerticalAlign.MIDDLE);
            imgStyle.setProperty("maxWidth", "100%");
            imgStyle.setProperty("maxHeight", "100%");

            if(property.hasEditObjectAction && value != null) {
                img.addClassName("selectedFileCellHasEdit");
            } else {
                img.removeClassName("selectedFileCellHasEdit");
            }
        }
        element.appendChild(wrapImage(img));

        return true;
    }

    private Element wrapImage(Element img) {
        Label dropFilesLabel = new Label();
        dropFilesLabel.setAutoHorizontalAlignment(HasAutoHorizontalAlignment.ALIGN_CENTER);
        dropFilesLabel.setWidth("100%");
        dropFilesLabel.setHeight("100%");

        DataGrid.initSinkDragDropEvents(dropFilesLabel);

        Element dropFilesLabelElement = dropFilesLabel.getElement();

        if(img != null) {
            dropFilesLabelElement.appendChild(img);
        } else {
            dropFilesLabel.setText(REQUIRED_VALUE);
        }

        return dropFilesLabel.getElement();
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        element.getStyle().clearPadding();
        element.removeClassName("text-based-value-required");
        element.setTitle("");

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

    public String format(PValue value) {
        return value == null ? "" : value.toString();
    }
}
