package lsfusion.gwt.client.form.property.cell.controller;

public enum CommitReason implements EndReason {

    BLURRED, ENTERPRESSED, FORCED, OTHER;

    public boolean isBlurred() {
        return this == BLURRED;
    }
}
