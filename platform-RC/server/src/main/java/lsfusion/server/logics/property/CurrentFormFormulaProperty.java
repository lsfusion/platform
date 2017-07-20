package lsfusion.server.logics.property;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.i18n.LocalizedString;

public class CurrentFormFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentFormFormulaProperty(ValueClass paramClass) {
        super(LocalizedString.create("{logics.property.current.form}"), SQLSession.formParam, paramClass.getUpSet());

        finalizeInit();
    }
}