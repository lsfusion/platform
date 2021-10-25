package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.grid.DataGrid;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.ReplaceCellEditor;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import static com.google.gwt.dom.client.BrowserEvents.BLUR;
import static com.google.gwt.dom.client.BrowserEvents.KEYDOWN;

public interface RequestReplaceCellEditor extends RequestCellEditor, ReplaceCellEditor {

    default void onBrowserEvent(Element parent, EventHandler handler) {
        Event event = handler.event;
        String type = event.getType();
        if (KEYDOWN.equals(type)) {
            int keyCode = event.getKeyCode();
            if (keyCode == KeyCodes.KEY_ENTER && checkEnterEvent(handler.event)) {
                handler.consume();
                commit(parent,  CommitReason.ENTERPRESSED);
            } else if (keyCode == KeyCodes.KEY_ESCAPE && GKeyStroke.isPlainKeyEvent(handler.event)) {
                handler.consume();
                cancel(parent);
            }
        } else if (BLUR.equals(type)) {
            // Cancel the change. Ensure that we are blurring the input element and
            // not the parent element itself.
            if (!DataGrid.isFakeBlur(event, parent)) {
                commit(parent, CommitReason.BLURRED);
            }
        }
    }

    default boolean checkEnterEvent(Event event) {
        return GKeyStroke.isPlainKeyEvent(event);
    }

    @Override
    default void clearRender(Element cellParent, RenderContext renderContext, boolean cancel) {
        GwtClientUtils.removeAllChildren(cellParent);
    }
}
