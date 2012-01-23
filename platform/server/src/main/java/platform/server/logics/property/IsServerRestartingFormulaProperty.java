package platform.server.logics.property;

import platform.server.classes.LogicalClass;
import platform.server.data.SQLSession;
import platform.server.logics.ServerResourceBundle;

public class IsServerRestartingFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public IsServerRestartingFormulaProperty(String sID) {
        super(sID, ServerResourceBundle.getString("logics.property.server.reboots"), SQLSession.isServerRestartingParam, LogicalClass.instance);

        finalizeInit();
    }
}
