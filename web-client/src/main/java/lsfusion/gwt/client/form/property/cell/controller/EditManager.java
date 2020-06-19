package lsfusion.gwt.client.form.property.cell.controller;

import lsfusion.gwt.client.form.property.cell.view.EditContext;

import java.util.function.Consumer;

public interface EditManager {
    default void commitEditing(Object value) {
        commitEditing(value, editContext -> {});
    }

    void commitEditing(Object value, Consumer<EditContext> afterFinish);

    void cancelEditing();
}
