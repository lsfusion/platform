package lsfusion.client.form.property.cell.classes.controller.suggest;

// see GCompletionType for extra description
public enum CompletionType {
    STRICT,
    SEMI_STRICT,
    NON_STRICT;

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
