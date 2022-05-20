package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.CellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;

public interface RequestCellEditor extends CellEditor {

    // force commit with the current value
    void commit(Element parent, CommitReason commitReason);

    // force cancel
    default void cancel(Element parent) {
        cancel(parent, CancelReason.FORCED);
    }
    void cancel(Element parent, CancelReason cancelReason);

    default void onBrowserEvent(Element parent, EventHandler handler) {
    }

    default void stop(Element parent, boolean cancel, boolean blurred) {
    }
}
