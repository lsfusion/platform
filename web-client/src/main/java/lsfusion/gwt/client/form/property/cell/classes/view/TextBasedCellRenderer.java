package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.object.table.grid.view.GPivot;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.EscapeUtils.unicodeEscape;
import static lsfusion.gwt.client.view.StyleDefaults.CELL_HORIZONTAL_PADDING;
import static lsfusion.gwt.client.view.StyleDefaults.TEXT_MULTILINE_PADDING;

public abstract class TextBasedCellRenderer<T> extends CellRenderer<T> {

    protected TextBasedCellRenderer(GPropertyDraw property) {
        super(property);
    }

    @Override
    protected boolean isSimpleText() {
        return !isMultiLine();
    }

    @Override
    protected String getDefaultVertAlignment() {
        if(isMultiLine())
            return "stretch";
        return super.getDefaultVertAlignment();
    }

    public void renderStaticContent(Element element, RenderContext renderContext) {
        render(property, element, renderContext, isMultiLine(), isWordWrap());
        GPivot.setTableToExcelAlignment(element, property);
    }

    public static void render(GPropertyDraw property, Element element, RenderContext renderContext, boolean multiLine, boolean wordWrap) {
        setPadding(element.getStyle(), multiLine);
        setBasedTextFonts(property, element, renderContext);
        if(wordWrap)
            element.getStyle().setProperty("wordBreak", "break-word"); // wordWrap (overflow-wrap) doesn't work as expected
    }

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
        element.getStyle().clearPadding();

        if(isWordWrap())
            element.getStyle().clearProperty("wordBreak");
    }

    protected boolean isMultiLine() {
        return false;
    }

    protected boolean isWordWrap() {
        return false;
    }

    @Override
    public int getWidthPadding() {
        return CELL_HORIZONTAL_PADDING;
    }
    public int getHeightPadding() {
        return getHeightPadding(isMultiLine());
    }

    public static int getHeightPadding(boolean multiLine) {
        if(multiLine)
            return TEXT_MULTILINE_PADDING;
        return 0;
    }
    public static void setPadding(Style style, boolean multiLine) {
        if(multiLine) {
            style.setPaddingTop(TEXT_MULTILINE_PADDING, Style.Unit.PX);
            style.setPaddingBottom(TEXT_MULTILINE_PADDING, Style.Unit.PX);

            style.setWhiteSpace(Style.WhiteSpace.PRE_WRAP);
        } else {
            // since we are aligning text with lineheight set vertical padding to 0
            style.setPaddingBottom(0, Style.Unit.PX);
            style.setPaddingTop(0, Style.Unit.PX);

            // we want to keep spaces
            style.setWhiteSpace(Style.WhiteSpace.PRE);
        }

        style.setPaddingRight(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
        style.setPaddingLeft(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
    }

    public static void setBasedTextFonts(GPropertyDraw property, Element element, RenderContext renderContext) {
        GFont font = property.font != null ? property.font : renderContext.getFont();

        if (font != null) {
            font.apply(element.getStyle());
            GPivot.setTableToExcelFontStyle(element, font);
        }
    }

    public void renderDynamicContent(Element element, Object value, UpdateContext updateContext) {
        if (value == null) {
            element.setTitle(property.isEditableNotNull() ? REQUIRED_VALUE : "");
            setInnerText(element, null);
        } else {
            String stringValue = unicodeEscape(format((T) value));
            setInnerText(element, stringValue);
            element.setTitle(property.echoSymbols ? "" : stringValue);
        }
    }

    protected abstract void setInnerText(Element element, String innerText);

    public abstract String format(T value);

    protected String getRequiredStringValue() {
        return MainFrame.showNotDefinedStrings ? REQUIRED_VALUE : "<div class=\"notNullLine\"></div>";
    }
}
