package lsfusion.gwt.client.grid;

public interface EditManager {
    void commitEditing(Object value);

    void cancelEditing();

    void selectNextCellInColumn(boolean down);
}
