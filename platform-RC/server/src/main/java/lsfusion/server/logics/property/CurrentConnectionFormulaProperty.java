package lsfusion.server.logics.property;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.i18n.LocalizedString;

public class CurrentConnectionFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentConnectionFormulaProperty(ValueClass paramClass) {
        super(LocalizedString.create("{logics.property.current.connection}"), SQLSession.connectionParam, paramClass.getUpSet());

        finalizeInit();
    }
}