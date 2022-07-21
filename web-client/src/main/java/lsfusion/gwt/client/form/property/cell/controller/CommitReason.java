package lsfusion.gwt.client.form.property.cell.controller;

public enum CommitReason implements EndReason {

    BLURRED,
    ENTERPRESSED,
    FORCED, // explicit value / ok commit, checkCommit (binding, changing row, EMBEDDED form)
    SUGGEST; // suggestion list

    public boolean isBlurred() {
        return this == BLURRED;
    }
}
