package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.CellRenderer;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;
import lsfusion.gwt.client.view.MainFrame;

import static lsfusion.gwt.client.base.EscapeUtils.unicodeEscape;
import static lsfusion.gwt.client.view.StyleDefaults.CELL_HORIZONTAL_PADDING;

public abstract class TextBasedCellRenderer<T> extends CellRenderer<T> {
    protected GPropertyDraw property;

    protected TextBasedCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    public void renderStatic(Element element, RenderContext renderContext) {
        Style style = element.getStyle();

        Style.TextAlign textAlignStyle = property.getTextAlignStyle();
        if (textAlignStyle != null) {
            style.setTextAlign(textAlignStyle);
        }

        renderStaticContent(element, renderContext);
    }

    protected void renderStaticContent(Element element, RenderContext renderContext) {
        Style style = element.getStyle();
        // важно оставить множественные пробелы
        style.setWhiteSpace(Style.WhiteSpace.PRE);
        style.setPosition(Style.Position.RELATIVE);

        setPadding(style);
    }

    private static void setPadding(Style style) {
        style.setPaddingBottom(0, Style.Unit.PX);
        style.setPaddingTop(0, Style.Unit.PX);

        style.setPaddingRight(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
        style.setPaddingLeft(CELL_HORIZONTAL_PADDING, Style.Unit.PX);
    }

    public static void setBasedTextFonts(GPropertyDraw property, Style style, UpdateContext updateContext) {
        GFont font = property.font != null ? property.font : updateContext.getFont();

        if (font != null) {
            font.apply(style);
        }
    }

    public void renderDynamic(Element element, Object value, UpdateContext updateContext) {
        setBasedTextFonts(property, element.getStyle(), updateContext);
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
