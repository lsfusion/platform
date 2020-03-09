package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.GridCellRenderer;

import static lsfusion.gwt.client.base.EscapeUtils.unicodeEscape;

public abstract class TextBasedGridCellRenderer<T> extends GridCellRenderer {
    protected GPropertyDraw property;

    TextBasedGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    public void renderStatic(Element element, GFont font, boolean isSingle) {
        Style style = getBasedStyle(element, font, isSingle);
        Style.TextAlign textAlignStyle = property.getTextAlignStyle();

        if (textAlignStyle != null) {
            style.setTextAlign(textAlignStyle);
        }

        style.setPaddingTop(0, Style.Unit.PX);
        style.setPaddingLeft(4, Style.Unit.PX);

        // важно оставить множественные пробелы
        style.setWhiteSpace(Style.WhiteSpace.PRE);
        style.setPosition(Style.Position.RELATIVE);

        //нужно для эллипсиса, но подтормаживает рендеринг,
        //оставлено закомменченым просто для справки
//        style.setOverflow(Style.Overflow.HIDDEN);
//        style.setTextOverflow(Style.TextOverflow.ELLIPSIS);

        style.clearProperty("lineHeight");
    }

    protected Style getBasedStyle(Element element, GFont font, boolean isSingle) {
        Style style = element.getStyle();
        style.setPaddingRight(4, Style.Unit.PX);
        style.setPaddingBottom(0, Style.Unit.PX);
        return style;
    }

    protected void setBasedTextFonts(Style style, GFont font, boolean isSingle) {
        if (property.font == null && isSingle) {
            property.font = font;
        }

        if (property.font != null) {
            property.font.apply(style);
        }
    }

    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        setBasedTextFonts(element.getStyle(), font, isSingle);
        if (value == null) {
            element.setTitle(property.isEditableNotNull() ? REQUIRED_VALUE : "");
            setInnerText(element, null);
        } else {
            String stringValue = unicodeEscape(castToString((T) value));
            setInnerText(element, stringValue);
            element.setTitle(property.echoSymbols ? "" : stringValue);
        }
    }

    protected abstract void setInnerText(Element element, String innerText);

    protected abstract String castToString(T value);
}
