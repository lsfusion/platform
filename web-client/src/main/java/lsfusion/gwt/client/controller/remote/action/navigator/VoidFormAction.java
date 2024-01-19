package lsfusion.gwt.client.controller.remote.action.navigator;

import lsfusion.gwt.client.controller.remote.action.form.FormRequestCountingAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;

public class VoidFormAction extends FormRequestCountingAction<ServerResponseResult> {
    public long waitRequestIndex;

    public VoidFormAction() {
    }

    public VoidFormAction(long waitRequestIndex) {
        this.waitRequestIndex = waitRequestIndex;
    }
}