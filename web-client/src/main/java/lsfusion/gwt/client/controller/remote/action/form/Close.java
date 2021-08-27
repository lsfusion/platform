package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.base.result.VoidResult;

public class Close extends FormPriorityAction<VoidResult> {

    public int closeDelay;

    @SuppressWarnings("UnusedDeclaration")
    public Close() {
    }

    public Close(int closeDelay) {
        this.closeDelay = closeDelay;
    }
}
