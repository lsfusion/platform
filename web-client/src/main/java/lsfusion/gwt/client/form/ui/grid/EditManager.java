package lsfusion.gwt.client.form.ui.grid;

public interface EditManager {
    void commitEditing(Object value);

    void cancelEditing();

    void selectNextCellInColumn(boolean down);
}
