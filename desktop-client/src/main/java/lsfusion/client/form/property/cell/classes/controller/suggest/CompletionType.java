package lsfusion.client.form.property.cell.classes.controller.suggest;

public enum CompletionType {
    STRICT, NON_STRICT, SEMI_STRICT;

    public boolean isStrict() {
        return this == STRICT;
    }

    public boolean isSemiStrict() {
        return this == SEMI_STRICT;
    }

    public boolean isAnyStrict() {
        return this == STRICT || this == SEMI_STRICT;
    }
}
