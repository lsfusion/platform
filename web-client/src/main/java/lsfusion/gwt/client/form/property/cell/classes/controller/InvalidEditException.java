package lsfusion.gwt.client.form.property.cell.classes.controller;

public class InvalidEditException extends Exception {
    public boolean patternMismatch;

    public InvalidEditException() {
        this(false);
    }

    public InvalidEditException(boolean patternMismatch) {
        this.patternMismatch = patternMismatch;
    }
}
