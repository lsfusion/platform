package platform.server.logics.linear.properties;

import platform.server.logics.properties.MultiplyFormulaProperty;
import platform.server.logics.properties.StringFormulaPropertyInterface;

public class LMFP extends LP<StringFormulaPropertyInterface, MultiplyFormulaProperty> {

    public LMFP(MultiplyFormulaProperty iProperty) {
        super(iProperty);
        listInterfaces.addAll(property.interfaces);
    }
}
