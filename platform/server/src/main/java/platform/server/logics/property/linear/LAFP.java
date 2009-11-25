package platform.server.logics.property.linear;

import platform.server.logics.property.AndFormulaProperty;
import platform.server.logics.property.FormulaPropertyInterface;

public class LAFP extends LP<FormulaPropertyInterface, AndFormulaProperty> {

    public LAFP(AndFormulaProperty iProperty) {
        super(iProperty);
    }
}
