package platform.server.logics.property;

import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImList;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetStaticValue;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.server.classes.StringClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.StringConcatenateExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.PropertyChanges;

public class StringConcatenateProperty extends FormulaProperty<StringConcatenateProperty.Interface> {
    private final String separator;
    private final boolean caseSensitive;

    public static class Interface extends PropertyInterface {
        public Interface(int ID) {
            super(ID);
        }
    }

    static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, new GetIndex<Interface>() {
            public Interface getMapValue(int i) {
                return new Interface(i);
            }});
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

    protected Expr calculateExpr(final ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        ImList<Expr> exprs =  getOrderInterfaces().mapListValues(new GetValue<Expr, Interface>() {
            public Expr getMapValue(Interface value) {
                return joinImplement.get(value);
            }});
        return StringConcatenateExpr.create(exprs, separator, caseSensitive);
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        if(commonValue!=null) {
            return interfaces.mapValues(new GetStaticValue<ValueClass>() {
                public ValueClass getMapValue() {
                    return StringClass.get(0); // немного бред но ладно
                }});
        }
        return super.getInterfaceCommonClasses(commonValue);
    }
}
