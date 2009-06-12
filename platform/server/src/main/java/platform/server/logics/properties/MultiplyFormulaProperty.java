package platform.server.logics.properties;

import platform.server.data.classes.ConcreteValueClass;
import platform.server.data.types.Type;

public class MultiplyFormulaProperty extends StringFormulaProperty {

    public MultiplyFormulaProperty(String sID, ConcreteValueClass iValue, int paramCount) {
        super(sID,iValue,"",paramCount);
        for(int i=0;i<paramCount;i++)
            formula = (formula.length()==0?"": formula +"*") + "prm"+(i+1);
    }
}
