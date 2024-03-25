package lsfusion.gwt.client.form.property.cell.controller;

public enum CancelReason implements EndReason {
    BLURRED,
    ESCAPE_PRESSED,
    FORCED,
    HIDE; // explicit close, server close / no open

    public boolean isBlurred() {
        return this == BLURRED;
    }
}
