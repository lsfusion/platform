package lsfusion.gwt.client.form.design;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.size.GFixedSize;
import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.GwtSharedUtils;

import java.util.HashMap;
import java.util.Objects;
import java.util.function.Function;

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
                while (getCalcMeasure(new GFontWidthString(font, GwtSharedUtils.replicate('0', charWidth + delta * 2))).first.getPivotSize() < pixelWidth) {
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

    private static final HashMap<GFontWidthString, Pair<GSize, GSize>> calculatedMeasures = new HashMap<>();
    private static Pair<GSize, GSize> getCalcMeasure(GFontWidthString fontWidth) {
        Pair<GSize, GSize> measure = calculatedMeasures.get(fontWidth);
        if(measure != null)
            return measure;

        final Element element = DOM.createDiv();

        Style style = element.getStyle();

        int fontSize = -1;
        GFont font = fontWidth.font;
        if(font != null) {
            fontSize = font.size;
            font.apply(style);
        }
        if(fontSize <= 0) {
            fontSize = 12;
            style.setFontSize(fontSize, Style.Unit.PX);
        }

        style.setDisplay(Style.Display.INLINE_BLOCK);
        style.setPadding(0, Style.Unit.PX);
        style.setPosition(Style.Position.ABSOLUTE);
        style.setVisibility(Style.Visibility.HIDDEN);

        // just in case
        style.setMargin(0, Style.Unit.PX);
        style.setBorderWidth(0, Style.Unit.PX);

//        style.setProperty("lineHeight", "normal");

        // we're setting convertSize to get relevant pixelSize
        if(GFixedSize.VALUE_TYPE != GFixedSize.Type.PX)
            style.setFontSize(GFixedSize.convertFontSize, Style.Unit.PX);

        String string = fontWidth.sampleString;
        element.setInnerText(string);
        style.setWhiteSpace(string.contains("\n") ? Style.WhiteSpace.PRE_WRAP : Style.WhiteSpace.PRE);

        int fFontSize = fontSize;
        measure = calcSize(element, size -> GSize.getCalcValueSize(size, fFontSize));
        calculatedMeasures.put(fontWidth, measure);
        return measure;
    }

    private static <T> Pair<T, T> calcSize(Element element, Function<Integer, T> sizeCalc) {
        final Element body = RootPanel.getBodyElement();
        DOM.appendChild(body, element);

        try {
            final int width = element.getOffsetWidth();
            final int height = element.getOffsetHeight();

            int roundedWidth = (int) Math.round((double) width);
            int roundedHeight = (int) Math.round((double) height);

            return new Pair<>(sizeCalc.apply(roundedWidth), sizeCalc.apply(roundedHeight));
        } finally {
            element.getParentElement().removeChild(element);
        }
    }

    public static GSize getStringWidth(GFont font, String widthString) {
        return getCalcMeasure(new GFontWidthString(font, widthString)).first;
    }

    public static GSize getStringHeight(GFont font, String heightString) {
        return getCalcMeasure(new GFontWidthString(font, heightString)).second;
    }

    private static class GridParams {

        private final int lineCount;
        private final int columnCount;
        private final boolean hasHeaders;
        private final boolean hasFooters;

        public GridParams(int lineCount, int columnCount, boolean hasHeaders, boolean hasFooters) {
            this.lineCount = lineCount;
            this.columnCount = columnCount;
            this.hasHeaders = hasHeaders;
            this.hasFooters = hasFooters;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof GridParams && lineCount == ((GridParams) o).lineCount &&
                    columnCount == ((GridParams) o).columnCount &&
                    hasHeaders == ((GridParams) o).hasHeaders &&
                    hasFooters == ((GridParams) o).hasFooters;
        }

        @Override
        public int hashCode() {
            return (31 * lineCount + columnCount) + (hasHeaders ? 13 : 0) + (hasFooters ? 5 : 0);
        }
    }
    private static final HashMap<GridParams, Pair<GSize, GSize>> calculatedPaddings = new HashMap<>();
    public static Pair<GSize, GSize> getGridPaddings(int linesCount, int columnCount, boolean hasHeaders, boolean hasFooters) {
        GridParams gridParams = new GridParams(linesCount, columnCount, hasHeaders, hasFooters);
        Pair<GSize, GSize> measure = calculatedPaddings.get(gridParams);
        if(measure != null)
            return measure;

        TableElement tableElement = Document.get().createTableElement();

        tableElement.getStyle().setProperty("width", "fit-content"); // because bootstrap sets table width to 100%
        tableElement.addClassName("table");

        if(hasHeaders) {
            TableSectionElement headerElement = tableElement.createTHead();
            addCells(headerElement.insertRow(-1), columnCount);
        }

        TableSectionElement bodyElement = GwtClientUtils.createTBody(tableElement);
        for(int i = 0; i < linesCount; i++)
            addCells(bodyElement.insertRow(-1), columnCount);

        if(hasFooters) {
            TableSectionElement headerElement = tableElement.createTHead();
            addCells(headerElement.insertRow(-1), columnCount);
        }

        int remSize = getRemSize();
        measure = calcSize(tableElement, size -> GSize.getCalcComponentSize(size, remSize));
        calculatedPaddings.put(gridParams, measure);
        return measure;
    }

    private static void addCells(TableRowElement headerRow, int cellsCount) {
        for(int i = 0; i< cellsCount; i++)
            headerRow.insertCell(-1);
    }

    private static Integer calculatedRemSize;
    private static int getRemSize() {
        if(calculatedRemSize == null) {
            final Element element = DOM.createDiv();
            element.getStyle().setProperty("width", "1rem");

            calculatedRemSize = calcSize(element, size -> size).first;
        }
        return calculatedRemSize;
    }

//    public interface MetricsCallback {
//        Widget metricsCalculated();
//    }
}
