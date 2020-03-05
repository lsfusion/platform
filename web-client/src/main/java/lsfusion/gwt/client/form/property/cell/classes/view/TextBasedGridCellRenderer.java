package lsfusion.gwt.client.form.property.cell.classes.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.AbstractGridCellRenderer;

import static lsfusion.gwt.client.base.EscapeUtils.unicodeEscape;

public abstract class TextBasedGridCellRenderer<T> extends AbstractGridCellRenderer {
    protected GPropertyDraw property;

    TextBasedGridCellRenderer(GPropertyDraw property) {
        this.property = property;
    }

    public void renderStatic(Element element, GFont font, boolean isSingle) {
        Style divStyle = getTextBasedStyle(element, font, isSingle);
        Style.TextAlign textAlignStyle = property.getTextAlignStyle();

        if (textAlignStyle != null) {
            divStyle.setTextAlign(textAlignStyle);
        }

        divStyle.setPaddingTop(0, Style.Unit.PX);
        divStyle.setPaddingLeft(4, Style.Unit.PX);

        // важно оставить множественные пробелы
        divStyle.setWhiteSpace(Style.WhiteSpace.PRE);
        divStyle.setPosition(Style.Position.RELATIVE);

        //нужно для эллипсиса, но подтормаживает рендеринг,
        //оставлено закомменченым просто для справки
//        divStyle.setOverflow(Style.Overflow.HIDDEN);
//        divStyle.setTextOverflow(Style.TextOverflow.ELLIPSIS);

        divStyle.clearProperty("lineHeight");
    }

    protected Style getTextBasedStyle(Element element, GFont font, boolean isMultiple) {
        Style divStyle = element.getStyle();
        divStyle.setPaddingRight(4, Style.Unit.PX);
        divStyle.setPaddingBottom(0, Style.Unit.PX);

        if (property.font == null && isMultiple) {
            property.font = font;
        }

        if (property.font != null) {
            property.font.apply(divStyle);
        }
        return divStyle;
    }

    public void renderDynamic(Element element, GFont font, Object value, boolean isSingle) {
        if (value == null) {
            element.setTitle(property.isEditableNotNull() ? REQUIRED_VALUE : "");
            setInnerText(element, null);
        } else {
            String stringValue = unicodeEscape(castToString((T) value));
            setInnerText(element, stringValue);
            element.setTitle(property.echoSymbols ? "" : stringValue);
        }
    }

    protected void setInnerText(Element div, String innerText) {
        if (innerText == null) {
            if (property.isEditableNotNull()) {
                div.setInnerText(REQUIRED_VALUE);
                div.addClassName("requiredValueString");
                div.removeClassName("nullValueString");
            } else {
                div.setInnerText(EMPTY_VALUE);
                div.addClassName("nullValueString");
                div.removeClassName("requiredValueString");
            }
        } else {
            div.setInnerText(innerText);
            div.removeClassName("nullValueString");
            div.removeClassName("requiredValueString");
        }
    }

    protected abstract String castToString(T value);
}
