package lsfusion.gwt.client.form.property.async;

import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

public class GPushAsyncChange extends GPushAsyncResult {
    
    public GUserInputResult result;

    public GPushAsyncChange() {
    }

    public GPushAsyncChange(GUserInputResult result) {
        this.result = result;
    }
}
