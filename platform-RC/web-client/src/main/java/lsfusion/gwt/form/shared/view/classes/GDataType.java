package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.shared.view.GFont;
import lsfusion.gwt.form.shared.view.GFontMetrics;
import lsfusion.gwt.form.shared.view.filter.GCompare;

import static lsfusion.gwt.form.shared.view.filter.GCompare.*;

public abstract class GDataType extends GType implements GClass {
    @Override
    public boolean hasChildren() {
        return false;
    }

    public String getMinimumMask(String pattern) {
        return getPreferredMask(pattern);
    }

    public abstract String getPreferredMask(String pattern);

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font, String pattern) {
        int minCharWidth = getMinimumCharWidth(minimumCharWidth, pattern);
        return minCharWidth * GFontMetrics.getZeroSymbolWidth(font == null || font.size == null? null : font) + 8;
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font, String pattern) {
        if (maximumCharWidth != 0) {
            return maximumCharWidth * GFontMetrics.getZeroSymbolWidth(font == null || font.size == null ? null : font) + 8;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font, String pattern) {
        int prefCharWidth = getPreferredCharWidth(preferredCharWidth, pattern);
        return prefCharWidth * GFontMetrics.getZeroSymbolWidth(font == null || font.size == null ? null : font) + 8;
    }

    public int getMinimumCharWidth(int definedMinimumCharWidth, String pattern) {
        return (definedMinimumCharWidth > 0 ? definedMinimumCharWidth : getMinimumMask(pattern).length());
    }

    public int getPreferredCharWidth(int definedPreferredCharWidth, String pattern) {
        return (definedPreferredCharWidth > 0 ? definedPreferredCharWidth : getPreferredMask(pattern).length());
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS};
    }
}
