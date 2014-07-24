package lsfusion.server.logics.property;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServerResourceBundle;

public class CurrentComputerFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentComputerFormulaProperty(ValueClass paramClass) {
        super(ServerResourceBundle.getString("logics.property.current.computer"), SQLSession.computerParam, paramClass.getUpSet());

        finalizeInit();
    }
}
