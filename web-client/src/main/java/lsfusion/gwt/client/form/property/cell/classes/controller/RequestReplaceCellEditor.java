package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.property.cell.controller.ReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

public interface RequestReplaceCellEditor extends RequestEmbeddedCellEditor, ReplaceCellEditor {

    @Override
    default void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        GwtClientUtils.removeAllChildren(cellParent);
    }
}
