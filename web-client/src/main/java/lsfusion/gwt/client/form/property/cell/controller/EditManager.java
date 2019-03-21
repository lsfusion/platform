package lsfusion.gwt.client.form.property.cell.controller;

public interface EditManager {
    void commitEditing(Object value);

    void cancelEditing();

    void selectNextCellInColumn(boolean down);
}
