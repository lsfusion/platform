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

    public abstract String getMask(String pattern);

    @Override
    public int getPixelWidth(int minimumCharWidth, GFont font, String pattern) {
        int charWidth = getCharWidth(minimumCharWidth, pattern);
        return charWidth * GFontMetrics.getZeroSymbolWidth(font == null || font.size == null? null : font) + 8;
    }

    public int getCharWidth(int definedMinimumCharWidth, String pattern) {
        return (definedMinimumCharWidth > 0 ? definedMinimumCharWidth : getMask(pattern).length());
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS};
    }
}
