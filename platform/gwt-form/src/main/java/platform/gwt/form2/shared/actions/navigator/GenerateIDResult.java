package platform.gwt.form2.shared.actions.navigator;

import net.customware.gwt.dispatch.shared.Result;

public class GenerateIDResult implements Result {
    public int ID;
    public GenerateIDResult() {
    }

    public GenerateIDResult(int ID) {
        this.ID = ID;
    }
}
