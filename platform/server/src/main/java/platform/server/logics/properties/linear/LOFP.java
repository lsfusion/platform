package platform.server.logics.properties.linear;

import platform.server.logics.properties.FormulaPropertyInterface;
import platform.server.logics.properties.ObjectFormulaProperty;

public class LOFP extends LP<FormulaPropertyInterface, ObjectFormulaProperty> {

    public LOFP(ObjectFormulaProperty iProperty) {
        super(iProperty);
    }
}
