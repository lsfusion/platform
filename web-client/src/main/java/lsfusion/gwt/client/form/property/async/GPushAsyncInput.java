package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

public class GPushAsyncInput extends GPushAsyncResult {
    
    public GUserInputResult result;

    public GPushAsyncInput() {
    }

    public GPushAsyncInput(GUserInputResult result) {
        this.result = result;
    }
}
