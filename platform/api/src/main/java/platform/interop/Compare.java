package platform.interop;

public class Compare {

    public static final int EQUALS = 0;
    public static final int GREATER = 1;
    public static final int LESS = 2;
    public static final int GREATER_EQUALS = 3;
    public static final int LESS_EQUALS = 4;
    public static final int NOT_EQUALS = 5;

    public static int not(int compare) {
        switch(compare) {
            case EQUALS: return NOT_EQUALS;
            case GREATER: return LESS_EQUALS;
            case LESS: return GREATER_EQUALS;
            case GREATER_EQUALS: return LESS;
            case LESS_EQUALS: return GREATER;
            default: throw new RuntimeException("Не должно быть");
        }
    }

    public static int reverse(int compare) {
        switch(compare) {
            case EQUALS: return EQUALS;
            case GREATER: return LESS;
            case LESS: return GREATER;
            case GREATER_EQUALS: return LESS_EQUALS;
            case LESS_EQUALS: return GREATER_EQUALS;
            default: throw new RuntimeException("Не должно быть");
        }
    }
}
