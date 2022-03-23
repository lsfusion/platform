package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public interface EditContext {

    RenderContext getRenderContext();
    UpdateContext getUpdateContext();

    GPropertyDraw getProperty();
    GGroupObjectValue getColumnKey();

    GGroupObjectValue getRowKey();

    Element getEditElement();
    Element getEditEventElement();

    default Object getValue() { return getUpdateContext().getValue(); }
    void setValue(Object value);

    default boolean isLoading() { return getUpdateContext().isLoading(); }
    void setLoading();

    Element getFocusElement();
    boolean isFocusable();
    Object forceSetFocus();
    void restoreSetFocus(Object forceSetFocus);
    boolean isSetLastBlurred();

    default void startEditing() {}
    default void stopEditing() {}
}
