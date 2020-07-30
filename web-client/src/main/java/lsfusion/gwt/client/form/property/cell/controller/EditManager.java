package lsfusion.gwt.client.form.property.cell.controller;

import java.util.function.Consumer;

public interface EditManager {
    default void commitEditing(Object value) {
        commitEditing(value, false);
    }
    void commitEditing(Object value, boolean blurred);  // assert if blurred then editor rerender dom

    void cancelEditing();
}
