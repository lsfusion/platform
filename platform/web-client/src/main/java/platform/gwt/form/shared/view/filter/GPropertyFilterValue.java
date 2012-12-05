package platform.gwt.form.shared.view.filter;

import platform.gwt.form.shared.view.GPropertyDraw;

public class GPropertyFilterValue extends GFilterValue {
    public GPropertyDraw property;

    @Override
    public String toString() {
        return "Свойство";
    }

    @Override
    public byte getTypeID() {
        return 2;
    }
}
