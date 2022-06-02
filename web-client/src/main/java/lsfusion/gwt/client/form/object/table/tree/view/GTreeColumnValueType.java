package lsfusion.gwt.client.form.object.table.tree.view;

public enum GTreeColumnValueType {

    OPEN, CLOSED, LEAF, LOADING;

    public static GTreeColumnValueType get(Boolean open) {
        if (open == null) {
            return LEAF;
        } else if (open) {
            return OPEN;
        } else {
            return CLOSED;
        }
    }
}
