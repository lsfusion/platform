package platform.server.logics.property;

import platform.server.classes.BaseClass;
import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;
import platform.server.logics.ServerResourceBundle;

public class CurrentSessionFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentSessionFormulaProperty(String sID, ValueClass valueClass) {
        super(sID, ServerResourceBundle.getString("logics.property.current.session"), SQLSession.sessionParam, valueClass.getUpSet());

        finalizeInit();
    }
}
