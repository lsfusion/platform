package lsfusion.base;

public class MultiCauseException extends RuntimeException {
    private final Throwable[] causes;

    public MultiCauseException(Throwable[] causes) {
        super(causes == null || causes.length == 0 ? null : causes[0]);
        this.causes = causes;
    }

    public Throwable[] getCauses() {
        return causes;
    }
}
