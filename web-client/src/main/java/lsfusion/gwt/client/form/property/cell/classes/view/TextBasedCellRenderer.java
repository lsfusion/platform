package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.view.StyleDefaults.CELL_HORIZONTAL_PADDING;
import static lsfusion.gwt.client.view.StyleDefaults.CELL_VERTICAL_PADDING;

public abstract class TextBasedCellRenderer<T> extends CellRenderer<T> {

    protected TextBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    public boolean renderContent(Element element, RenderContext renderContext) {
        setPadding(SimpleTextBasedCellRenderer.getSizeElement(element));

        if(property.isEditableNotNull())
            element.addClassName("requiredValueString");

        return false;
    }

    public static void clearRender(GPropertyDraw property, Element element, RenderContext renderContext) {
        element.getStyle().clearWhiteSpace();
    }

    @Override
    public boolean clearRenderContent(Element element, RenderContext renderContext) {
        clearPadding(SimpleTextBasedCellRenderer.getSizeElement(element));

        if (property.isEditableNotNull())
            element.removeClassName("requiredValueString");

        clearInnerText(element);

        return false;
    }

    protected boolean isMultiLine() {
        return false;
    }

    @Override
    public int getWidthPadding() {
        return CELL_HORIZONTAL_PADDING;
    }
    public int getHeightPadding() {
        return CELL_VERTICAL_PADDING;
    }
    public static void setPadding(Element element) {
        element.addClassName("text-based-prop");
//        Style style = element.getStyle();
//        style.setPaddingTop(CELL_VERTICAL_PADDING, Style.Unit.PX);
//        style.setPaddingBottom(CELL_VERTICAL_PADDING, Style.Unit.PX);
//        style.setPaddingRight(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
//        style.setPaddingLeft(CELL_HORIZONTAL_PADDING, Style.Unit.PX);

//        element.addClassName("form-control");
    }
    public static void clearPadding(Element element) {
        element.removeClassName("text-based-prop");
//        element.getStyle().clearPadding();

//        element.removeClassName("form-control");
    }

    public boolean updateContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
        return setInnerText(element, value != null ? format((T) value) : null);
    }

    public abstract String format(T value);

    protected boolean setInnerText(Element element, String innerText) {
        String title = property.echoSymbols ? "" : innerText;
        if(innerText == null) {
            element.addClassName("nullValueString");
            if(property.isEditableNotNull())
                title = REQUIRED_VALUE;
            innerText = "";
        } else {
            element.removeClassName("nullValueString");
            if(innerText.isEmpty()) {
                innerText = EMPTY_VALUE;
                element.addClassName("emptyValueString");
            } else
                element.removeClassName("emptyValueString");
        }

        element.setTitle(title);
        return setInnerContent(element, innerText);
    }

    public static void clearInnerText(Element element) {
        element.removeClassName("nullValueString");
        element.removeClassName("emptyValueString");
    }

    protected abstract boolean setInnerContent(Element element, String innerText);
}
