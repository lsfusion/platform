package lsfusion.gwt.client.form.property.cell.classes.controller.suggest;

public enum GCompletionType {
    ULTRA_STRICT, // only selected from the list
    STRICT, // selected from the list, or input string exactly the same as in the list
    SEMI_STRICT, // mix - select the first item, but allow entering any string value
    NON_STRICT, // any value, but can be selected from the list
    SEMI_ULTRA_NON_STRICT, // any value, but the value is not put into input (i.e either input or item selection)
    ULTRA_NON_STRICT; // any value can not be selected from the list

    public boolean isOnlyCommitSelection() {
        return this == ULTRA_STRICT;
    }

    public boolean isCheckCommitInputInList() {
        assert this != ULTRA_STRICT;
        return this == STRICT;
    }

    private boolean isAnyStrict() {
        return this == ULTRA_STRICT || this == STRICT || this == SEMI_STRICT;
    }

    public boolean isExactMatchNeeded() {
        return isAnyStrict();
    }

    public boolean isAutoSelection() {
        return isAnyStrict();
    }

//    public boolean commitSelectionOnEnter() {
//        return isAnyStrict();
//    }

    public boolean changeInputOnKeySelectionMove() {
        return this == NON_STRICT;
    }

    public boolean isCommitSelectionAllowed() {
        return this != ULTRA_NON_STRICT;
    }
}
