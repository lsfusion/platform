package platform.server.logics.properties.linear;

import platform.server.logics.properties.StringFormulaProperty;
import platform.server.logics.properties.StringFormulaPropertyInterface;

public class LSFP extends LP<StringFormulaPropertyInterface, StringFormulaProperty> {

    public LSFP(StringFormulaProperty iProperty,int paramCount) {
        super(iProperty);
    }
}
