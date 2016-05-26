package lsfusion.server.logics.property;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServerResourceBundle;

public class CurrentFormFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentFormFormulaProperty(ValueClass paramClass) {
        super(ServerResourceBundle.getString("logics.property.current.form"), SQLSession.formParam, paramClass.getUpSet());

        finalizeInit();
    }
}