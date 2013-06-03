package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.shared.view.GFont;
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
        return font == null || font.size == null ? minCharWidth * 7 : minCharWidth * font.size / 2;
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font) {
        if (maximumCharWidth != 0) {
            return font == null || font.size == null ? maximumCharWidth * 7 : maximumCharWidth * font.size / 2;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        int prefCharWidth = getPreferredCharWidth(preferredCharWidth);
        return font == null || font.size == null ? prefCharWidth * 7 : prefCharWidth * font.size / 2;
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
