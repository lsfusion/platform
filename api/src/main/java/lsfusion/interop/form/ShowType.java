package lsfusion.interop.form;

// временно так, потом возможно будет смысл отдельно разделить на DOCKED / FLOAT, NOWAIT / WAIT, and refactor DIALOG_MODAL to separate parameter
public enum ShowType {
    DOCKED, DOCKED_MODAL, MODAL, DIALOG_MODAL, EMBEDDED, POPUP;
}