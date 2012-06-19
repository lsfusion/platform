package platform.server.logics.property.derived;

import platform.server.classes.ConcatenateValueClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.ConcatenateExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.FormulaProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.PropertyChanges;

import java.util.*;

public class ConcatenateProperty extends FormulaProperty<ConcatenateProperty.Interface> {

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    static List<Interface> getInterfaces(int intNum) {
        List<Interface> interfaces = new ArrayList<Interface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new Interface(i));
        return interfaces;
    }

    public ConcatenateProperty(String sID, int intNum) {
        super(sID, "Concatenate " + intNum, getInterfaces(intNum));

        finalizeInit();
    }

    public Interface getInterface(int i) {
        Iterator<Interface> it = interfaces.iterator();
        for(int j=0;j<i;j++)
            it.next();
        return it.next();
    }

    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        List<Expr> exprs = new ArrayList<Expr>();
        for(int i=0;i<interfaces.size();i++) // assertion что порядок сохранился
            exprs.add(joinImplement.get(getInterface(i)));
        return ConcatenateExpr.create(exprs);
    }

    @Override
    public Map<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        if(commonValue!=null) {
            Map<Interface, ValueClass> result = new HashMap<Interface, ValueClass>();
            for(int i=0;i<interfaces.size();i++)
                result.put(getInterface(i), ((ConcatenateValueClass)commonValue).get(i));
            return result;
        }            
        return super.getInterfaceCommonClasses(commonValue);
    }
}
