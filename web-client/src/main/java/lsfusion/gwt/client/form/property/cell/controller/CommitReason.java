package lsfusion.gwt.client.form.property.cell.controller;

public enum CommitReason implements EndReason {

    BLURRED,
    ENTERPRESSED,
    FORCED, // explicit value / ok commit
    FORCED_BLURRED; // checkCommit (binding, changing row, EMBEDDED form)

    public boolean isBlurred() {
        return this == BLURRED;
    }

    public boolean isForcedBlurred() {
        return this == FORCED_BLURRED;
    }
}
