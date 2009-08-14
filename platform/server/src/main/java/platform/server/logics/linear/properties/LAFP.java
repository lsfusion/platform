package platform.server.logics.linear.properties;

import platform.server.logics.properties.AndFormulaProperty;
import platform.server.logics.properties.FormulaPropertyInterface;

public class LAFP extends LP<FormulaPropertyInterface, AndFormulaProperty> {

    public LAFP(AndFormulaProperty iProperty) {
        super(iProperty);
    }
}
