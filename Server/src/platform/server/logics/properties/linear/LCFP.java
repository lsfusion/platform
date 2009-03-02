package platform.server.logics.properties.linear;

import platform.server.logics.properties.CompareFormulaProperty;
import platform.server.logics.properties.FormulaPropertyInterface;

public class LCFP extends LP<FormulaPropertyInterface, CompareFormulaProperty> {

    public LCFP(CompareFormulaProperty iProperty) {
        super(iProperty);
        listInterfaces.add(property.operator1);
        listInterfaces.add(property.operator2);
    }
}
