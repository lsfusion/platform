package lsfusion.gwt.client.form.filter.user;

import lsfusion.gwt.client.ClientMessages;

public enum GCompare {
    EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS, CONTAINS, MATCH, INARRAY;

    public static GCompare get(boolean min) {
        return min? GCompare.LESS: GCompare.GREATER;
    }

    public static GCompare get(int compare) {
        switch(compare) {
            case 0:
                return EQUALS;
            case 1:
                return GREATER;
            case 2:
                return LESS;
            case 3:
                return GREATER_EQUALS;
            case 4:
                return LESS_EQUALS;
            case 5:
                return NOT_EQUALS;
            case 6:
                return CONTAINS;
            case 7:
                return MATCH;
            case 8:
                return INARRAY;
            default:
                return EQUALS;
        }
    }

    public byte serialize() {
        switch(this) {
            case EQUALS:
                return 0;
            case GREATER:
                return 1;
            case LESS:
                return 2;
            case GREATER_EQUALS:
                return 3;
            case LESS_EQUALS:
                return 4;
            case NOT_EQUALS:
                return 5;
            case CONTAINS:
                return 6;
            case MATCH:
                return 7;
            case INARRAY:
                return 8;
        }
        throw new RuntimeException("Serialize Compare");
    }

    @Override
    public String toString() {
        switch (this) {
            case EQUALS :
                return "=";
            case GREATER :
                return ">";
            case LESS :
                return "<";
            case GREATER_EQUALS :
                return ">=";
            case LESS_EQUALS :
                return "<=";
            case NOT_EQUALS :
                return "!=";
            case CONTAINS:
                return "_";
            case MATCH:
                return "@";
            case INARRAY :
                return "IN ARRAY";
        }
        throw new RuntimeException("Serialize Compare");
    }

    public String getFullString() {
        switch (this) {
            case EQUALS :
                return "=";
            case GREATER :
                return ">";
            case LESS :
                return "<";
            case GREATER_EQUALS :
                return ">=";
            case LESS_EQUALS :
                return "<=";
            case NOT_EQUALS :
                return "!= (" + ClientMessages.Instance.get().formFilterCompareNotEquals() + ")";
            case CONTAINS:
                return "_ (" + ClientMessages.Instance.get().formFilterCompareContains() + ")";
            case MATCH:
                return "@ (" + ClientMessages.Instance.get().formFilterCompareSearch() + ")";
            case INARRAY :
                return ClientMessages.Instance.get().formFilterCompareInArray();
        }
        return "";
    }
    
    public String getTooltipText() {
        switch (this) {
            case EQUALS :
                return ClientMessages.Instance.get().formFilterCompareEquals();
            case GREATER :
                return ClientMessages.Instance.get().formFilterCompareGreater();
            case LESS :
                return ClientMessages.Instance.get().formFilterCompareLess();
            case GREATER_EQUALS :
                return ClientMessages.Instance.get().formFilterCompareGreaterEquals();
            case LESS_EQUALS :
                return ClientMessages.Instance.get().formFilterCompareLessEquals();
            case NOT_EQUALS :
                return ClientMessages.Instance.get().formFilterCompareNotEquals();
            case CONTAINS:
                return ClientMessages.Instance.get().formFilterCompareContains();
            case MATCH:
                return ClientMessages.Instance.get().formFilterCompareSearch();
            case INARRAY :
                return ClientMessages.Instance.get().formFilterCompareInArray();
        }
        return "";
    }

    public boolean escapeComma() {
        return this == EQUALS || this == CONTAINS || this == MATCH;
    }
}
