package lsfusion.gwt.client.form.property.cell.view;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.ui.HasAutoHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public abstract class FileBasedCellRenderer extends CellRenderer {
    protected static final String ICON_EMPTY = "empty.png";
    protected static final String ICON_FILE = "file.png";

    @Override
    public void renderStaticContent(Element element, RenderContext renderContext) {
    }

    @Override
    public void renderDynamicContent(Element element, Object value, UpdateContext updateContext) {
        element.setInnerText(null);
        element.removeAllChildren();

        if (value == null && property.isEditableNotNull()) {
            setBasedEmptyElement(element);
        } else {
            element.getStyle().clearPadding();
            element.removeClassName("requiredValueString");
            element.setTitle("");

            Label dropFilesLabel = new Label();
            dropFilesLabel.setAutoHorizontalAlignment(HasAutoHorizontalAlignment.ALIGN_CENTER);
            dropFilesLabel.setWidth("100%");

            dropFilesLabel.addDragOverHandler(event -> {
            });
            dropFilesLabel.addDragLeaveHandler(event -> {
            });
            dropFilesLabel.addDropHandler(event -> {
            });

            Element dropFilesLabelElement = dropFilesLabel.getElement();
            element.appendChild(dropFilesLabelElement);

            InputElement inputElement = dropFilesLabelElement.appendChild(Document.get().createFileInputElement());
            inputElement.setId("input");
            inputElement.getStyle().setDisplay(Style.Display.NONE);

            ImageElement img = dropFilesLabelElement.appendChild(Document.get().createImageElement());

            Style imgStyle = img.getStyle();
            imgStyle.setVerticalAlign(Style.VerticalAlign.MIDDLE);
            imgStyle.setProperty("maxWidth", "100%");
            imgStyle.setProperty("maxHeight", "100%");

            if(property.hasEditObjectAction) {
                img.addClassName("selectedFileCellHasEdit");
            } else {
                img.removeClassName("selectedFileCellHasEdit");
            }

            img.setSrc(getFilePath(value));
        }
    }

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
        element.getStyle().clearPadding();
        element.removeClassName("requiredValueString");
        element.setTitle("");
    }

    protected void setBasedEmptyElement(Element element) {
        element.getStyle().setPaddingRight(4, Style.Unit.PX);
        element.getStyle().setPaddingLeft(4, Style.Unit.PX);
        element.setInnerText(REQUIRED_VALUE);
        element.setTitle(REQUIRED_VALUE);
        element.addClassName("requiredValueString");
    }

    protected abstract String getFilePath(Object value);

    protected FileBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    public String format(Object value) {
        return value == null ? "" : value.toString();
    }
}
