package lsfusion.gwt.client.action;

public class GCopyToClipboardAction extends GExecuteAction {
    public String value;

    @SuppressWarnings("UnusedDeclaration")
    public GCopyToClipboardAction() {}

    public GCopyToClipboardAction(String value) {
        this.value = value;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        copyToClipboard(value);
    }

    private native void copyToClipboard(String value)/*-{
        // navigator clipboard api needs a secure context (https)
        if (navigator.clipboard && window.isSecureContext) {
            //writeText work in Firefox
            navigator.clipboard.writeText(value).then(
                function () {
                }, function () { //Fallback for Chrome
                    @lsfusion.gwt.client.action.GCopyToClipboardAction::copyToClipboardTextArea(*)(value);
                }
            );
        } else {
            //execCommand for http
            @lsfusion.gwt.client.action.GCopyToClipboardAction::copyToClipboardTextArea(*)(value);
        }

    }-*/;

    private static native void copyToClipboardTextArea(String value)/*-{
        if (document.queryCommandSupported && document.queryCommandSupported("copy")) {
            var textarea = document.createElement("textarea");
            textarea.textContent = value;
            document.body.appendChild(textarea);
            textarea.focus();
            textarea.select();
            document.execCommand("copy");
            document.body.removeChild(textarea);
        }
    }-*/;
}