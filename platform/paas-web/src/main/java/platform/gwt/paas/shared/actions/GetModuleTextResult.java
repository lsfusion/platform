package platform.gwt.paas.shared.actions;

import net.customware.gwt.dispatch.shared.Result;

public class GetModuleTextResult implements Result {
    public String text;

    public GetModuleTextResult() {
    }

    public GetModuleTextResult(String text) {
        this.text = text;
    }
}
