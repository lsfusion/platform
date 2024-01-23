package lsfusion.gwt.client.action;

import lsfusion.gwt.client.base.view.CopyPasteUtils;

public class GCopyToClipboardAction extends GExecuteAction {
    public String value;

    @SuppressWarnings("UnusedDeclaration")
    public GCopyToClipboardAction() {}

    public GCopyToClipboardAction(String value) {
        this.value = value;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) throws Throwable {
        CopyPasteUtils.copyToClipboard(value);
    }
}