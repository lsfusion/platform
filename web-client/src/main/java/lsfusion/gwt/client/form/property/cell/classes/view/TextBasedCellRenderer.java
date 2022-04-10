package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.form.design.GFont;
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
    public boolean isSimpleText(RenderContext renderContext) {
        return !isMultiLine();
    }

    @Override
    public boolean isSimpleText(UpdateContext updateContext) {
        return !isMultiLine();
    }

    @Override
    protected String getDefaultVertAlignment() {
        if(isMultiLine())
            return "stretch";
        return super.getDefaultVertAlignment();
    }

    public void renderStaticContent(Element element, RenderContext renderContext) {
        render(property, element, renderContext, isMultiLine());
    }

    public static void render(GPropertyDraw property, Element element, RenderContext renderContext, boolean multiLine) {
        Style style = element.getStyle();
        setPadding(style, multiLine);
        style.setWhiteSpace(multiLine ? Style.WhiteSpace.PRE_WRAP : Style.WhiteSpace.PRE);
        setBasedTextFonts(property, element, renderContext);
    }

    @Override
    public void clearRenderContent(Element element, RenderContext renderContext) {
        element.getStyle().clearPadding();
        clearBasedTextFonts(property, element.getStyle(), renderContext);
    }

    protected boolean isMultiLine() {
        return false;
    }

    @Override
    public int getWidthPadding() {
        return CELL_HORIZONTAL_PADDING;
    }
    public int getHeightPadding() {
        if(isMultiLine())
            return TEXT_MULTILINE_PADDING;
        return super.getHeightPadding();
    }
    public static void setPadding(Style style, boolean multiLine) {
        if(multiLine) {
            style.setPaddingTop(TEXT_MULTILINE_PADDING, Style.Unit.PX);
            style.setPaddingBottom(TEXT_MULTILINE_PADDING, Style.Unit.PX);
        } else {
            // since we are aligning text with lineheight set vertical padding to 0
            style.setPaddingBottom(0, Style.Unit.PX);
            style.setPaddingTop(0, Style.Unit.PX);
        }

        style.setPaddingRight(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
        style.setPaddingLeft(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
    }

    public static void setBasedTextFonts(GPropertyDraw property, Element element, RenderContext renderContext) {
        GFont font = property.font != null ? property.font : renderContext.getFont();

        if (font != null) {
            font.apply(element.getStyle());
        }
    }
    public static void clearBasedTextFonts(GPropertyDraw property, Style style, RenderContext renderContext) {
        GFont font = property.font != null ? property.font : renderContext.getFont();

        if (font != null) {
            font.clear(style);
        }
    }

    public boolean renderDynamicContent(Element element, Object value, boolean loading, UpdateContext updateContext) {
        if (value == null) {
            element.setTitle(property.isEditableNotNull() ? REQUIRED_VALUE : "");
            setInnerText(element, null);
        } else {
            String stringValue = unicodeEscape(format((T) value));
            setInnerText(element, stringValue);
            element.setTitle(property.echoSymbols ? "" : stringValue);
        }

        return true;
    }

    public abstract String format(T value);

    protected void setInnerText(Element element, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                setInnerHTML(element, getRequiredStringValue(element));
                element.addClassName("requiredValueString");
            } else {
                setInnerContent(element, getNullStringValue(element));
                element.removeClassName("requiredValueString");
            }
        } else {
            setInnerContent(element, getNotNullStringValue(innerText, element));
            element.removeClassName("requiredValueString");
        }
    }

    protected String getRequiredStringValue(Element element) {
        return MainFrame.showNotDefinedStrings ? REQUIRED_VALUE : "<div class=\"notNullLine\">" + EscapeUtils.UNICODE_NBSP + "</div>";
    }

    protected String getNullStringValue(Element element) {
        return EscapeUtils.UNICODE_NBSP;
    }

    protected String getNotNullStringValue(String innerText, Element element) {
        assert !innerText.isEmpty();
        return innerText;
    }

    protected void setInnerContent(Element element, String innerText) {
        assert !innerText.isEmpty(); // important to make paste work (otherwise DataGrid.sinkPasteEvent cannot put empty selection)
        element.setInnerText(innerText);
    }

    protected void setInnerHTML(Element element, String innerHTML) {
        // assert that innerHTML has text inside, important to make paste work (otherwise DataGrid.sinkPasteEvent cannot put empty selection)
        element.setInnerHTML(innerHTML);
    }

    @Override
    public boolean isAutoDynamicHeight() {
        return isMultiLine();
    }
}
