package lsfusion.gwt.client.form.property.cell.controller;

public enum CommitReason implements EndReason {

    BLURRED,
    ENTER_PRESSED,
    FORCED, // explicit value / ok commit
    FORCED_BLURRED; // checkCommit (binding, changing row, EMBEDDED form)

    public boolean isCancelIfInvalid() {
        return this != ENTER_PRESSED;
    }

    public CancelReason cancel() {
        return this == BLURRED ? CancelReason.BLURRED : CancelReason.FORCED;
    }

    public boolean isBlurred() {
        return this == BLURRED;
    }

    public boolean isForcedBlurred() {
        return this == FORCED_BLURRED;
    }
}
