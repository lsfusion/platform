package lsfusion.gwt.client.form.grid;

public interface EditManager {
    void commitEditing(Object value);

    void cancelEditing();

    void selectNextCellInColumn(boolean down);
}
