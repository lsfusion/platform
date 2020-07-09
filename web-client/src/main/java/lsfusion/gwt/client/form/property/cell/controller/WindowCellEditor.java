package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.dom.client.Element;

public interface WindowCellEditor extends KeepCellEditor {

    @Override
    default void commitEditing(Element parent) { // actually it's not used now but just in case
        commit();
    }

    void commit();
}
