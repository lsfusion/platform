package platform.gwt.form.shared.view.classes;

import platform.gwt.form.shared.view.filter.GCompare;

import static platform.gwt.form.shared.view.filter.GCompare.EQUALS;
import static platform.gwt.form.shared.view.filter.GCompare.NOT_EQUALS;

public abstract class GFileType extends GDataType {
    @Override
    public String getPreferredMask() {
        return "1234567";
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, NOT_EQUALS};
    }
}
