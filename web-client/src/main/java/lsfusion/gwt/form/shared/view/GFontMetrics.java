package lsfusion.gwt.form.shared.view;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;

import java.util.ArrayList;
import java.util.HashMap;

public class GFontMetrics {
    private static final HashMap<MetricsCallback, Integer> calculationsInProgress = new HashMap<MetricsCallback, Integer>();

    // все шрифты, с которыми приходилось работать на клиенте
    private static final HashMap<GFont, FontMeasure> calculatedFonts = new HashMap<GFont, FontMeasure>();

    private static boolean isCalculated(GFont font) {
        return calculatedFonts.containsKey(font);
    }

    private static FontMeasure getMeasure(GFont font) {
        return calculatedFonts.get(font);
    }

    private static int getCalculationsCount(MetricsCallback callback) {
        Integer count = calculationsInProgress.get(callback);
        if (count == null) {
            calculationsInProgress.put(callback, 0);
            return 0;
        }
        return count;
    }

    private static void setCalculationsCount(MetricsCallback callback, int count) {
        calculationsInProgress.put(callback, count);
    }

    private static void calculationFinished(MetricsCallback callback) {
        setCalculationsCount(callback, getCalculationsCount(callback) - 1);

        if (getCalculationsCount(callback) == 0) {
            calculationsInProgress.remove(callback);
            callback.metricsCalculated();
        }
    }

    private static void calculationStarted(MetricsCallback callback) {
        setCalculationsCount(callback, getCalculationsCount(callback) + 1);
    }

    public static void calculateFontMetrics(ArrayList<GFont> fonts, MetricsCallback callback) {
        fonts.add(GFont.DEFAULT_FONT);

        boolean allCalculated = true;
        for (GFont font : fonts) {
            if (font != null && !isCalculated(font)) {
                allCalculated = false;
                calculate(font, callback);
                calculationStarted(callback);
            }
        }
        if (allCalculated) {
            callback.metricsCalculated();
        }
    }

    private static void calculate(final GFont font, final MetricsCallback callback) {
        final Element element = DOM.createSpan();

        Style style = element.getStyle();

        style.setDisplay(Style.Display.INLINE);
        style.setMargin(0, Style.Unit.PX);
        style.setBorderWidth(0, Style.Unit.PX);
        style.setPadding(0, Style.Unit.PX);
        style.setVisibility(Style.Visibility.HIDDEN);
        style.setPosition(Style.Position.ABSOLUTE);
        style.setWhiteSpace(Style.WhiteSpace.PRE);

        font.apply(style);

        final String text = "0";
        DOM.setInnerText(element, text);

        final Element body = RootPanel.getBodyElement();
        DOM.appendChild(body, element);

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                finishCalculate(font, text, element, callback);
            }
        });
    }

    private static void finishCalculate(GFont font, String text, final Element element, MetricsCallback callback) {
        try {
            final int width = element.getOffsetWidth() / text.length();
            final int height = element.getOffsetHeight();

            calculatedFonts.put(font, new FontMeasure((int)Math.round((double) width), (int)Math.round((double) height)));

            calculationFinished(callback);
        } finally {
            // dont want element to remain regardless whether or not
            // measurements succeeded.
            element.getParentElement().removeChild(element);
        }
    }

    public static int getZeroSymbolWidth(GFont font) {
        FontMeasure measure = getMeasure(font == null ? GFont.DEFAULT_FONT : font);
        return measure != null ? measure.width : 0;
    }

    public static int getSymbolHeight(GFont font) {
        FontMeasure measure = getMeasure(font == null ? GFont.DEFAULT_FONT : font);
        return measure != null ? measure.height : 0;
    }

    private static class FontMeasure {
        final int width;
        final int height;

        private FontMeasure(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    public interface MetricsCallback {
        void metricsCalculated();
    }
}
