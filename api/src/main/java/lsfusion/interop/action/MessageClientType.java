package lsfusion.interop.action;

public enum MessageClientType {
    INFO, // MESSAGE LOG, PRINT MESSAGE (no cancel in transaction) -> notify + log
    SUCCESS,
    WARN, // MESSAGE, INTERNAL ExecutionContext.message -> message, notify if not active
    ERROR, // PRINT MESSAGE -> message, notify if not active + log
    DEFAULT;
}
