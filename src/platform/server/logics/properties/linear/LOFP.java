package platform.server.logics.properties.linear;

import platform.server.logics.properties.FormulaPropertyInterface;
import platform.server.logics.properties.ObjectFormulaProperty;

public class LOFP extends LP<FormulaPropertyInterface, ObjectFormulaProperty> {

    public LOFP(ObjectFormulaProperty iProperty,int bitCount) {
        super(iProperty);
        listInterfaces.add(property.objectInterface);
        for(int i=0;i<bitCount;i++) {
            FormulaPropertyInterface propertyInterface = new FormulaPropertyInterface(listInterfaces.size());
            listInterfaces.add(propertyInterface);
            property.interfaces.add(propertyInterface);
        }
    }
}
