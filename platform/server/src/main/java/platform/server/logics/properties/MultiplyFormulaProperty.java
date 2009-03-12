package platform.server.logics.properties;

import platform.server.logics.classes.RemoteClass;
import platform.server.logics.data.TableFactory;

import java.util.Collection;
import java.util.ArrayList;

public class MultiplyFormulaProperty extends StringFormulaProperty {

    public MultiplyFormulaProperty(String sID, TableFactory iTableFactory, RemoteClass iValue,int params) {
        super(sID,params,iTableFactory,iValue,"");
        for(int i=0;i<params;i++)
            formula = (formula.length()==0?"": formula +"*") + "prm"+(i+1);
    }

    RemoteClass getOperandClass() {
        return RemoteClass.integral;
    }
}
