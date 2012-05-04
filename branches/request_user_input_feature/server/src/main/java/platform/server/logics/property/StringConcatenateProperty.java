package platform.server.logics.property;

import platform.server.data.expr.Expr;
import platform.server.data.expr.StringConcatenateExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class StringConcatenateProperty extends FormulaProperty<StringConcatenateProperty.Interface> {
    private final String separator;
    private final boolean caseSensitive;

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

    public StringConcatenateProperty(String sID, String caption, int intNum, String separator) {
        this(sID, caption, intNum, separator, true);
    }

    public StringConcatenateProperty(String sID, String caption, int intNum, String separator, boolean caseSensitive) {
        super(sID, caption, getInterfaces(intNum));
        this.separator = separator;
        this.caseSensitive = caseSensitive;

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
        return StringConcatenateExpr.create(exprs, separator, caseSensitive);
    }
}
