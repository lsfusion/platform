package lsfusion.gwt.form.shared.actions.logics;

import net.customware.gwt.dispatch.shared.Result;

public class GenerateIDResult implements Result {
    public long ID;
    public GenerateIDResult() {
    }

    public GenerateIDResult(long ID) {
        this.ID = ID;
    }
}
