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

    public String getMinimumMask() {
        return getPreferredMask();
    }

    public abstract String getPreferredMask();

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        int minCharWidth = getMinimumCharWidth(minimumCharWidth);
        return minCharWidth * GFontMetrics.getZeroSymbolWidth(font == null || font.size == null? null : font) + 8;
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font) {
        if (maximumCharWidth != 0) {
            return maximumCharWidth * GFontMetrics.getZeroSymbolWidth(font == null || font.size == null ? null : font) + 8;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        int prefCharWidth = getPreferredCharWidth(preferredCharWidth);
        return prefCharWidth * GFontMetrics.getZeroSymbolWidth(font == null || font.size == null ? null : font) + 8;
    }

    public int getMinimumCharWidth(int definedMinimumCharWidth) {
        return (definedMinimumCharWidth > 0 ? definedMinimumCharWidth : getMinimumMask().length());
    }

    public int getPreferredCharWidth(int definedPreferredCharWidth) {
        return (definedPreferredCharWidth > 0 ? definedPreferredCharWidth : getPreferredMask().length());
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS};
    }
}
