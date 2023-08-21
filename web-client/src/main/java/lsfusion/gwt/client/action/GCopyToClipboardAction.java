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
        //writeText work in Firefox
        //execCommand work in Chrome
        navigator.clipboard.writeText(value).then(
            function () {
            }, function () {
                if (document.queryCommandSupported && document.queryCommandSupported("copy")) {
                    var textarea = document.createElement("textarea");
                    textarea.textContent = value;
                    document.body.appendChild(textarea);
                    textarea.focus();
                    textarea.select();
                    document.execCommand("copy");
                    document.body.removeChild(textarea);
                }
            }
        );

    }-*/;
}