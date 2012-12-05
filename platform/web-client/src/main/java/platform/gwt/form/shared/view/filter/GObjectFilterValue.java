package platform.gwt.form.shared.view.filter;

import platform.gwt.form.shared.view.GObject;

public class GObjectFilterValue extends GFilterValue {
    public GObject object;

    @Override
    public String toString() {
        return "Объект";
    }

    @Override
    public byte getTypeID() {
        return 1;
    }
}
