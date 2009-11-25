package platform.server.logics.property.linear;

import platform.server.logics.property.CompareFormulaProperty;
import platform.server.logics.property.FormulaPropertyInterface;

public class LCFP extends LP<FormulaPropertyInterface, CompareFormulaProperty> {

    public LCFP(CompareFormulaProperty iProperty) {
        super(iProperty);
    }
}
