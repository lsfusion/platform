package lsfusion.server.logics.property;

import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServerResourceBundle;

public class IsFullClientFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public final static IsFullClientFormulaProperty instance = new IsFullClientFormulaProperty();

    private IsFullClientFormulaProperty() {
        super(ServerResourceBundle.getString("logics.property.current.isfullclient"), SQLSession.isFullClientParam, LogicalClass.instance);

        finalizeInit();
    }
}
