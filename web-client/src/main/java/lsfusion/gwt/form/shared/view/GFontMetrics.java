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

    public static ArrayList<GFont> registeredFonts = new ArrayList<GFont>();

    private static int calculationsInProgress = 0;

    // все шрифты, с которыми приходилось работать на клиенте
    private static final HashMap<GFont, HashMap<String, Integer>> calculatedFonts = new HashMap<GFont, HashMap<String, Integer>>();

    public GFontMetrics get() {
        return INSTANCE;
    }

    public static void registerFont(GFont font) {
        for (GFont registeredFont : registeredFonts) {
            if (registeredFont.equalsForMetrics(font)) {
                return;
            }
        }
        registeredFonts.add(font);
    }

    public static void calculateFontMetrics(ArrayList<GFont> fonts, MetricsCallback callback) {
        fonts.add(DEFAULT_FONT);
        for (GFont font : fonts) {
            if (!isCalculated(font)) {
                calculationsInProgress++;
                getFontMetrics(font, font.family, font.size, font.isBold(), callback);
            }
        }
        if (calculationsInProgress == 0) {
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
            @lsfusion.gwt.form.shared.view.GFontMetrics::setFontMetrics(Llsfusion/gwt/form/shared/view/GFont;Ljava/lang/String;Ljava/lang/String;Llsfusion/gwt/form/shared/view/GFontMetrics$MetricsCallback;)(gfont, String(fontMeasure.width), String(fontMeasure.leading), callback);
        }

        font.onerror = function() {
            @lsfusion.gwt.form.shared.view.GFontMetrics::errorLoadingFont(Llsfusion/gwt/form/shared/view/GFont;Llsfusion/gwt/form/shared/view/GFontMetrics$MetricsCallback;)(gfont, callback);
        }
    }-*/;

    public static void setFontMetrics(GFont font, String width, String height, MetricsCallback callback) {
        HashMap<String, Integer> measures = new HashMap<String, Integer>();
        measures.put(WIDTH_KEY, Integer.valueOf(width));
        measures.put(HEIGHT_KEY, Integer.valueOf(height));
        calculatedFonts.put(font, measures);
        calculationsInProgress--;

        if (calculationsInProgress == 0) {
            callback.metricsCalculated();
        }
    }

    public static void errorLoadingFont(GFont font, MetricsCallback callback) {
        // сталкивался лишь с ошибкой нераспознанной fontFamily
        getFontMetrics(font, DEFAULT_FONT_FAMILY, font.size, font.isBold(), callback);
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
            if (font.equalsForMetrics(ifont)) {
                return calculatedFonts.get(font);
            }
        }
        return null;
    }

    public interface MetricsCallback {
        void metricsCalculated();
    }
}
