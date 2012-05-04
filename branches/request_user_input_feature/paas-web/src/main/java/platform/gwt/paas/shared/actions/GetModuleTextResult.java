package platform.gwt.paas.shared.actions;

import com.gwtplatform.dispatch.shared.Result;

public class GetModuleTextResult implements Result {
    public String text;

    public GetModuleTextResult() {
    }

    public GetModuleTextResult(String text) {
        this.text = text;
    }
}
