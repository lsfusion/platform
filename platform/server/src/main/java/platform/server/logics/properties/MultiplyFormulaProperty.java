package platform.server.logics.properties;

import platform.server.logics.classes.RemoteClass;
import platform.server.logics.data.TableFactory;

public class MultiplyFormulaProperty extends StringFormulaProperty {

    public MultiplyFormulaProperty(TableFactory iTableFactory, RemoteClass iValue,int Params) {
        super(iTableFactory,iValue,"");
        for(int i=0;i<Params;i++) {
            interfaces.add(new StringFormulaPropertyInterface(i));
            formula = (formula.length()==0?"": formula +"*") + "prm"+(i+1);
        }
    }

    RemoteClass getOperandClass() {
        return RemoteClass.integral;
    }
}
