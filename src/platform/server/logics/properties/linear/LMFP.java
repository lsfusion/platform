package platform.server.logics.properties.linear;

import platform.server.logics.properties.StringFormulaPropertyInterface;
import platform.server.logics.properties.MultiplyFormulaProperty;

public class LMFP extends LP<StringFormulaPropertyInterface, MultiplyFormulaProperty> {

    public LMFP(MultiplyFormulaProperty iProperty) {
        super(iProperty);
        listInterfaces.addAll(property.interfaces);
    }
}
