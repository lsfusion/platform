package lsfusion.gwt.client.form.property.cell.controller;

import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.classes.GType;

public interface ExecuteEditContext extends EditContext {

    void setLoading();

    Boolean isReadOnly();

    void focus(FocusUtils.Reason reason); // assert is focusable

    default boolean canUseChangeValueForRendering(GType type) {
        return getProperty().canUseChangeValueForRendering(type, getRenderContext().getRendererType());
    }
}
