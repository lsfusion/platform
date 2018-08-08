package lsfusion.gwt.form.shared.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.HashMap;

public class GFontMetrics {
    private static final HashMap<GFontWidthString, FontMeasure> calculatedMeasures = new HashMap<>();

    private static FontMeasure getCalcMeasure(GFontWidthString font) {
        FontMeasure measure = calculatedMeasures.get(font);
        if(measure != null)
            return measure;

        final Element element = DOM.createSpan();

        Style style = element.getStyle();

        style.setDisplay(Style.Display.INLINE);
        style.setMargin(0, Style.Unit.PX);
        style.setBorderWidth(0, Style.Unit.PX);
        style.setPadding(0, Style.Unit.PX);
        style.setVisibility(Style.Visibility.HIDDEN);
        style.setPosition(Style.Position.ABSOLUTE);
        style.setWhiteSpace(Style.WhiteSpace.PRE);

        font.font.apply(style);

        final String text = font.widthString == null ? "0" : font.widthString;
        element.setInnerText(text);

        final com.google.gwt.dom.client.Element body = RootPanel.getBodyElement();
        DOM.appendChild(body, element);

        try {
            final int width = element.getOffsetWidth();
            final int height = element.getOffsetHeight();

            measure = new FontMeasure((int) Math.round((double) width), (int) Math.round((double) height));
        } finally {
            element.getParentElement().removeChild(element);
        }
        calculatedMeasures.put(font, measure);
        return measure;
    }

    public static int getStringWidth(GFontWidthString fontWidthString) {
        return getCalcMeasure(fontWidthString).width;
    }

    public static int getSymbolHeight(GFont font) {
        FontMeasure measure = getCalcMeasure(font == null ? GFontWidthString.DEFAULT_FONT : new GFontWidthString(font));
        return measure != null ? measure.height : 0;
    }

    public static int getSymbolWidth(GFont font) {
        FontMeasure measure = getCalcMeasure(font == null ? GFontWidthString.DEFAULT_FONT : new GFontWidthString(font));
        return measure != null ? measure.width : 0;
    }

    private static class FontMeasure {
        final int width;
        final int height;

        private FontMeasure(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
