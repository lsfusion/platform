package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.filter.GCompare;

import static platform.gwt.form.shared.view.filter.GCompare.*;

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
    public int getMinimumPixelWidth(int minimumCharWidth) {
        return (minimumCharWidth > 0 ? minimumCharWidth : getMinimumMask().length()) * 12;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth) {
        return (preferredCharWidth > 0 ? preferredCharWidth : getPreferredMask().length()) * 12;
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS};
    }
}
