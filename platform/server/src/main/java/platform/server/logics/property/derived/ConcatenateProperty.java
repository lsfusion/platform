package platform.server.logics.property.derived;

import platform.server.data.expr.ConcatenateExpr;
import platform.server.data.expr.Expr;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.property.FormulaProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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

    protected ConcatenateProperty(int intNum) {
        super("CONCATENATE_"+intNum, "Concatenate " + intNum, getInterfaces(intNum));
    }

    public Interface getInterface(int i) {
        Iterator<Interface> it = interfaces.iterator();
        for(int j=0;j<i;j++)
            it.next();
        return it.next();
    }

    protected Expr calculateExpr(Map<Interface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        List<Expr> exprs = new ArrayList<Expr>();
        for(int i=0;i<interfaces.size();i++) // assertion что порядок сохранился
            exprs.add(joinImplement.get(getInterface(i)));
        return ConcatenateExpr.create(exprs);
    }
}
