package lsfusion.gwt.client.form.property.cell.controller;

import lsfusion.gwt.client.classes.GType;

public interface ExecuteEditContext extends EditContext {

    void setLoading();

    Boolean isReadOnly();

    void trySetFocusOnBinding();

    default boolean canUseChangeValueForRendering(GType type) {
        return getProperty().canUseChangeValueForRendering(type, getRenderContext().getRendererType());
    }
}
