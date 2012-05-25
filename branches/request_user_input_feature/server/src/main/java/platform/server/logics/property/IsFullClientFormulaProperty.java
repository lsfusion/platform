package platform.server.logics.property;

import platform.server.classes.LogicalClass;
import platform.server.data.SQLSession;
import platform.server.logics.ServerResourceBundle;

public class IsFullClientFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public final static IsFullClientFormulaProperty instance = new IsFullClientFormulaProperty();

    private IsFullClientFormulaProperty() {
        super("IsFullClient", ServerResourceBundle.getString("logics.property.current.isfullclient"), SQLSession.isFullClientParam, LogicalClass.instance);

        finalizeInit();
    }
}
