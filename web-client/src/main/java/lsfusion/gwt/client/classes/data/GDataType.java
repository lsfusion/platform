package lsfusion.gwt.client.classes.data;

import lsfusion.gwt.client.base.size.GSize;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.classes.GClass;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.design.GFont;
import lsfusion.gwt.client.form.filter.user.GCompare;
import lsfusion.gwt.client.form.property.GPropertyDraw;

import static lsfusion.gwt.client.form.filter.user.GCompare.*;

public abstract class GDataType extends GType implements GClass {
    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public GCompare[] getFilterCompares() {
        return new GCompare[] {EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS};
    }
}
