package lsfusion.server.logics.property;

import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.i18n.LocalizedString;

public class IsFullClientFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public final static IsFullClientFormulaProperty instance = new IsFullClientFormulaProperty();

    private IsFullClientFormulaProperty() {
        super(LocalizedString.create("{logics.property.current.isfullclient}"), SQLSession.isFullClientParam, LogicalClass.instance);

        finalizeInit();
    }
}
