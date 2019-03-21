package lsfusion.gwt.shared.action;

public class GCopyToClipboardAction implements GAction {
    public String value;

    @SuppressWarnings("UnusedDeclaration")
    public GCopyToClipboardAction() {}

    public GCopyToClipboardAction(String value) {
        this.value = value;
    }

    @Override
    public Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        //копирование в буфер обмена реализовано только в desktop, web пока не удалось
        return false;
    }
}