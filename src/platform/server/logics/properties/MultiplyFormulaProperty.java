package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.logics.classes.DataClass;

public class MultiplyFormulaProperty extends StringFormulaProperty {

    public MultiplyFormulaProperty(TableFactory iTableFactory, DataClass iValue,int Params) {
        super(iTableFactory,iValue,"");
        for(int i=0;i<Params;i++) {
            interfaces.add(new StringFormulaPropertyInterface(i));
            Formula = (Formula.length()==0?"":Formula+"*") + "prm"+(i+1);
        }
    }

    DataClass getOperandClass() {
        return DataClass.integral;
    }
}
