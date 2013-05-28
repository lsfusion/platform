package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.filter.GCompare;

import java.text.ParseException;

public abstract class GAbstractStringType extends GDataType {

    public boolean caseInsensitive;

    protected GAbstractStringType() {}

    protected GAbstractStringType(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    @Override
    public GCompare[] getFilterCompares() {
        return GCompare.values();
    }

    @Override
    public Object parseString(String s) throws ParseException {
        return s;
    }

    @Override
    public GCompare getDefaultCompare() {
        return GCompare.CONTAINS;
    }

    @Override
    public int getMinimumPixelWidth(int minimumCharWidth, GFont font) {
        int minCharWidth = getMinimumCharWidth(minimumCharWidth);
        return font == null || font.size == null ? minCharWidth * 10 : minCharWidth * font.size * 5 / 8;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth, GFont font) {
        int prefCharWidth = getPreferredCharWidth(preferredCharWidth);
        return font == null || font.size == null ? prefCharWidth * 10 : prefCharWidth * font.size * 5 / 8;
    }
}
