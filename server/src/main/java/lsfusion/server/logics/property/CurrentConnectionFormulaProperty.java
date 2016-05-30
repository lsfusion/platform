package lsfusion.server.logics.property;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServerResourceBundle;

public class CurrentConnectionFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentConnectionFormulaProperty(ValueClass paramClass) {
        super(ServerResourceBundle.getString("logics.property.current.connection"), SQLSession.connectionParam, paramClass.getUpSet());

        finalizeInit();
    }
}