package platform.server.logics.property;

import platform.server.classes.ConcreteValueClass;

public class MultiplyFormulaProperty extends StringFormulaProperty {

    private static String getFormula(int paramCount) {
        String formula = "";
        for(int i=0;i<paramCount;i++)
            formula = (formula.length()==0?"": formula +"*") + "prm"+(i+1);
        return formula;
    }
    public MultiplyFormulaProperty(String sID, ConcreteValueClass value, int paramCount) {
        super(sID,value,getFormula(paramCount),paramCount);
    }
}
