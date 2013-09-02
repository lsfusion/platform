package lsfusion.gwt.form.shared.view;

import java.util.ArrayList;
import java.util.HashMap;

public class GFontMetrics {
    private final GFontMetrics INSTANCE = new GFontMetrics();
    private static final String WIDTH_KEY = "width";
    private static final String HEIGHT_KEY = "height";

    private static final String DEFAULT_FONT_FAMILY = "Arial";
    private static final int DEFAULT_FONT_PIXEL_SIZE = 11;
    public static final GFont DEFAULT_FONT = new GFont(null, null, DEFAULT_FONT_PIXEL_SIZE, DEFAULT_FONT_FAMILY);

    private static HashMap<MetricsCallback, Integer> calculationsInProgress = new HashMap<MetricsCallback, Integer>();

    // все шрифты, с которыми приходилось работать на клиенте
    private static final HashMap<GFont, HashMap<String, Integer>> calculatedFonts = new HashMap<GFont, HashMap<String, Integer>>();

    public GFontMetrics get() {
        return INSTANCE;
    }

    private static Integer getCalculationsCount(MetricsCallback callback) {
        Integer count = calculationsInProgress.get(callback);
        if (count == null) {
            calculationsInProgress.put(callback, 0);
            return 0;
        }
        return count;
    }

    private static void setCalculationsCount(MetricsCallback callback, Integer count) {
        calculationsInProgress.put(callback, count);
    }

    public static void calculateFontMetrics(ArrayList<GFont> fonts, MetricsCallback callback) {
        fonts.add(DEFAULT_FONT);
        for (GFont font : fonts) {
            if (!isCalculated(font)) {
                setCalculationsCount(callback, getCalculationsCount(callback) + 1);
                getFontMetrics(font, font.family, font.size, font.isBold(), callback);
            }
        }
        if (getCalculationsCount(callback) == 0) {
            calculationsInProgress.remove(callback);
            callback.metricsCalculated();
        }
    }

    private static boolean isCalculated(GFont font) {
        return getMeasure(font) != null;
    }

    private static native void getFontMetrics(GFont gfont, String fontFamily, int fontSize, boolean bold, MetricsCallback callback) /*-{
        var font = new $wnd.Font();
        font.fontFamily = fontFamily;
        if (bold) {
            font.fontFamily += " bold";
        }
        font.src = font.fontFamily;
        font.onload = function() {
            var fontMeasure = font.measureText("0", fontSize);
            @lsfusion.gwt.form.shared.view.GFontMetrics::setFontMetrics(Llsfusion/gwt/form/shared/view/GFont;DDLlsfusion/gwt/form/shared/view/GFontMetrics$MetricsCallback;)(gfont, fontMeasure.width, fontMeasure.leading, callback);
        }

        font.onerror = function(errorMessage) {
            @lsfusion.gwt.form.shared.view.GFontMetrics::errorLoadingFont(Llsfusion/gwt/form/shared/view/GFont;Llsfusion/gwt/form/shared/view/GFontMetrics$MetricsCallback;)(gfont, callback);
        }
    }-*/;

    public static void setFontMetrics(GFont font, double width, double height, MetricsCallback callback) {
        HashMap<String, Integer> measures = new HashMap<String, Integer>();
        measures.put(WIDTH_KEY, (int)Math.round(width));
        measures.put(HEIGHT_KEY, (int)Math.round(height));
        calculatedFonts.put(font, measures);
        setCalculationsCount(callback, getCalculationsCount(callback) - 1);

        if (getCalculationsCount(callback) == 0) {
            calculationsInProgress.remove(callback);
            callback.metricsCalculated();
        }
    }

    public static void errorLoadingFont(GFont font, MetricsCallback callback) {
        if (font.isBold()) {
            //пытаемся подгрузить не-bold версию шрифта
            getFontMetrics(font, font.family, font.size, false, callback);
        } else {
            //используем шрифт по умолчанию
            getFontMetrics(font, DEFAULT_FONT_FAMILY, font.size, false, callback);
        }
    }

    public static int getZeroSymbolWidth(GFont font) {
        HashMap<String, Integer> measure = getMeasure(font == null ? DEFAULT_FONT : font);
        if (measure != null) {
            return measure.get(WIDTH_KEY);
        }
        return 0;
    }

    public static int getSymbolHeight(GFont font) {
        HashMap<String, Integer> measure = getMeasure(font == null ? DEFAULT_FONT : font);
        if (measure != null) {
            return measure.get(HEIGHT_KEY);
        }
        return 0;
    }

    private static HashMap<String, Integer> getMeasure(GFont ifont) {
        for (GFont font : calculatedFonts.keySet()) {
            if (font.equals(ifont)) {
                return calculatedFonts.get(font);
            }
        }
        return null;
    }

    public interface MetricsCallback {
        void metricsCalculated();
    }
}
