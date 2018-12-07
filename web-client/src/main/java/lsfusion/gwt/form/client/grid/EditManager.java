package lsfusion.gwt.form.client.grid;

public interface EditManager {
    void commitEditing(Object value);

    void cancelEditing();

    void selectNextCellInColumn(boolean down);
}
