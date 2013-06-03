package lsfusion.server.logics.property;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServerResourceBundle;

public class CurrentComputerFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentComputerFormulaProperty(String sID, ValueClass paramClass) {
        super(sID, ServerResourceBundle.getString("logics.property.current.computer"), SQLSession.computerParam, paramClass.getUpSet());

        finalizeInit();
    }
}
