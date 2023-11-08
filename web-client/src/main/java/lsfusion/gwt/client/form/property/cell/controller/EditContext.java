package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.RendererType;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

public interface EditContext extends ExecContext {

    RenderContext getRenderContext();
    UpdateContext getUpdateContext();

    GPropertyDraw getProperty();
    GGroupObjectValue getColumnKey();

    GGroupObjectValue getRowKey();

    default GGroupObjectValue getFullKey() {
        GPropertyDraw property = getProperty();
        GGroupObjectValue rowKey = property.isList ? getRowKey() : GGroupObjectValue.EMPTY; // because for example in custom renderer editContext can be not the currentKey
        return GGroupObjectValue.getFullKey(rowKey, getColumnKey());
    }

    Element getEditElement();

    default PValue getValue() { return getUpdateContext().getValue(); }
    void setValue(PValue value);

    Element getFocusElement();
    boolean isFocusable();
    Object forceSetFocus();
    void restoreSetFocus(Object forceSetFocus);
    boolean isSetLastBlurred();

    default void startEditing() {}
    default void stopEditing() {}

    boolean canUseChangeValueForRendering(GType type);

    default Object modifyPastedString(String pastedText) { return pastedText; }

    RendererType getRendererType();
}
