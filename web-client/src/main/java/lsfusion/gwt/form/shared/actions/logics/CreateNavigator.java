package lsfusion.gwt.form.shared.actions.logics;

import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.shared.actions.logics.LogicsAction;
import net.customware.gwt.dispatch.shared.general.StringResult;

public class CreateNavigator extends LogicsAction<StringResult> {
    public String tabSID;
    
    public CreateNavigator() {}
    
    public CreateNavigator(String tabSID) {
        this.tabSID = tabSID;
    }
}
