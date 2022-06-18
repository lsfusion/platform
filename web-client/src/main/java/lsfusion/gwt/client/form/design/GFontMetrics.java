package lsfusion.gwt.client.form.design;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.GwtSharedUtils;

import java.util.HashMap;

public class GFontMetrics {
    //    private static final HashMap<MetricsCallback, Integer> calculationsInProgress = new HashMap<>();
//
//    // все шрифты, с которыми приходилось работать на клиенте
//    private static final HashMap<GFontWidthString, FontMeasure> calculatedFonts = new HashMap<>();
//
//    private static boolean isCalculated(GFontWidthString font) {
//        return calculatedFonts.containsKey(font);
//    }
//
//    private static FontMeasure getMeasure(GFontWidthString font) {
//        return calculatedFonts.get(font);
//    }
//
//    private static int getCalculationsCount(MetricsCallSendRequestEDIActionPropertyback callback) {
//        Integer count = calculationsInProgress.get(callback);
//        if (count == null) {
//            calculationsInProgress.put(callback, 0);
//            return 0;
//        }
//        return count;
//    }
//
//    private static void setCalculationsCount(MetricsCallback callback, int count) {
//        calculationsInProgress.put(callback, count);
//    }
//
//    private static void calculationFinished(MetricsCallback callback) {
//        setCalculationsCount(callback, getCalculationsCount(callback) - 1);
//
//        if (getCalculationsCount(callback) == 0) {
//            calculationsInProgress.remove(callback);
//            callback.metricsCalculated();
//        }
//    }
//
//    private static void calculationStarted(MetricsCallback callback) {
//        setCalculationsCount(callback, getCalculationsCount(callback) + 1);
//    }
//
//    public static Widget calculateFontMetrics(ArrayList<GFontWidthString> fonts, MetricsCallback callback) {
//        fonts.add(GFontWidthString.DEFAULT_FONT);
//
//        boolean allCalculated = true;
//        for (GFontWidthString font : fonts) {
//            if (font != null && !isCalculated(font)) {
//                allCalculated = false;
//                calculate(font, callback);
//                calculationStarted(callback);
//            }
//        }
//        if (allCalculated) {
//            return callback.metricsCalculated();
//        }
//        return null;
//    }
//
//    private static void calculate(final GFontWidthString font, final MetricsCallback callback) {
//        final Element element = DOM.createSpan();
//
//        Style style = element.getStyle();
//
//        style.setDisplay(Style.Display.INLINE);
//        style.setMargin(0, Style.Unit.PX);
//        style.setBorderWidth(0, Style.Unit.PX);
//        style.setPadding(0, Style.Unit.PX);
//        style.setVisibility(Style.Visibility.HIDDEN);
//        style.setPosition(Style.Position.ABSOLUTE);
//        style.setWhiteSpace(Style.WhiteSpace.PRE);
//
//        font.font.apply(style);
//
//        final String text = font.widthString == null ? "0" : font.widthString;
//        element.setInnerText(text);
//
//        final com.google.gwt.dom.client.Element body = RootPanel.getBodyElement();
//        DOM.appendChild(body, element);
//
//        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
//            @Override
//            public void execute() {
//                finishCalculate(font, element, callback);
//            }
//        });
//    }
//
//    private static void finishCalculate(GFontWidthString font, final Element element, MetricsCallback callback) {
//        try {
//            final int width = element.getOffsetWidth();
//            final int height = element.getOffsetHeight();
//
//            calculatedFonts.put(font, new FontMeasure((int)Math.round((double) width), (int)Math.round((double) height)));
//
//            calculationFinished(callback);
//        } finally {
//            // dont want element to remain regardless whether or not
//            // measurements succeeded.
//            element.getParentElement().removeChild(element);
//        }
//    }
//
    private static final HashMap<GFontWidthString, FontMeasure> calculatedMeasures = new HashMap<>();
    private static final HashMap<GFont, HashMap<Integer, Integer>> calculatedCharWidth = new HashMap<>();

    public static int getCharWidthString(GFont font, int pixelWidth) {
        HashMap<Integer, Integer> widthMap = calculatedCharWidth.getOrDefault(font, new HashMap<>());
        Integer charWidth = widthMap.get(pixelWidth);
        if(charWidth != null) {
            return charWidth;
        } else {
            charWidth = 0;
            int delta = 1;

            while (delta >= 1) {
                while (getCalcMeasure(new GFontWidthString(font, GwtSharedUtils.replicate('0', charWidth + delta * 2))).width.getPivotSize() < pixelWidth) {
                    delta = delta * 2;
                }
                charWidth += delta;
                delta = delta == 1 ? 0 : 1;
            }
            widthMap.put(pixelWidth, charWidth);
            calculatedCharWidth.put(font, widthMap);
            return charWidth;
        }
    }

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

            GSize widthSize = GSize.getValueSize((int) Math.round((double) width)).add(GSize.TEMP_PADDING_ADJ);
            GSize heightSize = GSize.getValueSize((int) Math.round((double) height)).add(GSize.TEMP_PADDING_ADJ);

            measure = new FontMeasure(widthSize, heightSize);
        } finally {
            element.getParentElement().removeChild(element);
        }
        calculatedMeasures.put(font, measure);
        return measure;
    }

    public static GSize getStringWidth(GFontWidthString fontWidthString) {
        return getCalcMeasure(fontWidthString).width;
    }

    public static GSize getSymbolHeight(GFont font) {
        return getCalcMeasure(font == null ? GFontWidthString.DEFAULT_FONT : new GFontWidthString(font)).height;
    }

    public static GSize getSymbolWidth(GFont font) {
        return getCalcMeasure(font == null ? GFontWidthString.DEFAULT_FONT : new GFontWidthString(font)).width;
    }

    private static class FontMeasure {
        final GSize width;
        final GSize height;

        private FontMeasure(GSize width, GSize height) {
            this.width = width;
            this.height = height;
        }
    }

//    public interface MetricsCallback {
//        Widget metricsCalculated();
//    }
}
