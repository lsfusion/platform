package platform.server.logics.linear.properties;

import platform.server.logics.properties.CompareFormulaProperty;
import platform.server.logics.properties.FormulaPropertyInterface;

public class LCFP extends LP<FormulaPropertyInterface, CompareFormulaProperty> {

    public LCFP(CompareFormulaProperty iProperty) {
        super(iProperty);
    }
}
