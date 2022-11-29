package lsfusion.interop.logics;

import java.io.Serializable;

public class Settings implements Serializable {
    private final long busyDialogTimeout;
    private final boolean showNotDefinedStrings;
    private final boolean pivotOnlySelectedColumn;
    private final String matchSearchSeparator;
    private final boolean useTextAsFilterSeparator;

    public Settings(long busyDialogTimeout, boolean showNotDefinedStrings, boolean pivotOnlySelectedColumn, String matchSearchSeparator, boolean useTextAsFilterSeparator) {
        this.busyDialogTimeout = busyDialogTimeout;
        this.showNotDefinedStrings = showNotDefinedStrings;
        this.pivotOnlySelectedColumn = pivotOnlySelectedColumn;
        this.matchSearchSeparator = matchSearchSeparator;
        this.useTextAsFilterSeparator = useTextAsFilterSeparator;
    }

    public long getBusyDialogTimeout() {
        return busyDialogTimeout;
    }

    public boolean isShowNotDefinedStrings() {
        return showNotDefinedStrings;
    }

    public boolean isPivotOnlySelectedColumn() {
        return pivotOnlySelectedColumn;
    }

    public String getMatchSearchSeparator() {
        return matchSearchSeparator;
    }

    public boolean isUseTextAsFilterSeparator() {
        return useTextAsFilterSeparator;
    }
}
