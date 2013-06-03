package lsfusion.gwt.form.shared.view.filter;

public enum GCompare {
    EQUALS, GREATER, LESS, GREATER_EQUALS, LESS_EQUALS, NOT_EQUALS, START_WITH, CONTAINS, LIKE, INARRAY;

    public static GCompare get(boolean min) {
        return min? GCompare.LESS: GCompare.GREATER;
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
            case START_WITH:
                return 6;
            case CONTAINS:
                return 7;
            case LIKE:
                return 8;
            case INARRAY:
                return 9;
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
            case START_WITH :
                return "Начинается с";
            case CONTAINS:
                return "Содержит";
            case LIKE :
                return "LIKE";
            case INARRAY :
                return "IN ARRAY";
        }
        throw new RuntimeException("Serialize Compare");
    }
}
