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
        return getMinimumCharWidth(minimumCharWidth) * 7;
    }

    @Override
    public int getPreferredPixelWidth(int preferredCharWidth) {
        return getPreferredCharWidth(preferredCharWidth) * 7;
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
