package platform.server.logics.properties.linear;

import platform.server.logics.properties.FormulaPropertyInterface;
import platform.server.logics.properties.AndFormulaProperty;

public class LAFP extends LP<FormulaPropertyInterface, AndFormulaProperty> {

    public LAFP(AndFormulaProperty iProperty) {
        super(iProperty);
    }
}
