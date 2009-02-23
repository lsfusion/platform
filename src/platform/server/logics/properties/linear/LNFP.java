package platform.server.logics.properties.linear;

import platform.server.logics.properties.FormulaPropertyInterface;
import platform.server.logics.properties.NotNullFormulaProperty;

public class LNFP extends LP<FormulaPropertyInterface, NotNullFormulaProperty> {

    public LNFP(NotNullFormulaProperty iProperty) {
        super(iProperty);
        listInterfaces.add(property.property);
    }
}
