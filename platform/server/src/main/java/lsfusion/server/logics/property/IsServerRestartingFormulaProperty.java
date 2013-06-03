package lsfusion.server.logics.property;

import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServerResourceBundle;

public class IsServerRestartingFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public IsServerRestartingFormulaProperty(String sID) {
        super(sID, ServerResourceBundle.getString("logics.property.server.reboots"), SQLSession.isServerRestartingParam, LogicalClass.instance);

        finalizeInit();
    }
}
