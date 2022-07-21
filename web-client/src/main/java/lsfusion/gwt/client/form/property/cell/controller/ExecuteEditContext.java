package lsfusion.gwt.client.form.property.cell.controller;

import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.property.GPropertyDraw;

public interface ExecuteEditContext extends EditContext {

    void setLoading();

    boolean isReadOnly();

    void trySetFocusOnBinding();

    default boolean canUseChangeValueForRendering(GType type) {
        return getProperty().canUseChangeValueForRendering(type);
    }
}
