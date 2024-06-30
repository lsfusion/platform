package lsfusion.interop.action;

public enum MessageClientType {
    INFO, // MESSAGE LOG, PRINT MESSAGE (no cancel in transaction) -> notify + log
    WARN, WARN_EXTENDED, // MESSAGE, INTERNAL ExecutionContext.message (WARN_EXTENDED only for desktop) -> message, notify if not active
    ERROR; // PRINT MESSAGE -> message, notify if not active + log

    public static MessageClientType SYSTEM(boolean extended) {
        return extended ? WARN_EXTENDED : WARN;
    }
}
