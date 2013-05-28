package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.logics.ServerResourceBundle;
import platform.server.session.PropertyChanges;

// выбирает объект по битам
public class AndFormulaProperty extends FormulaProperty<AndFormulaProperty.Interface> {

    public final ObjectInterface objectInterface;
    public final ImSet<AndInterface> andInterfaces;

    public static abstract class Interface<P extends Interface<P>> extends PropertyInterface<P> {
        public Interface(int ID) {
            super(ID);
        }
    }

    public static class ObjectInterface extends Interface<ObjectInterface> {
        public ObjectInterface(int ID) {
            super(ID);
        }
    }

    public static class AndInterface extends Interface<AndInterface> {
        public AndInterface(int ID) {
            super(ID);
        }
    }

    static ImOrderSet<Interface> getInterfaces(int size) {
        return SetFact.toOrderExclSet(size + 1, new GetIndex<Interface>() {
            public Interface getMapValue(int i) {
                return i == 0 ? new ObjectInterface(0) : new AndInterface(i);
            }});
    }

    public AndFormulaProperty(String sID, int size) {
        super(sID, ServerResourceBundle.getString("logics.property.if"), getInterfaces(size));
        objectInterface = (ObjectInterface) getOrderInterfaces().get(0);
        andInterfaces = BaseUtils.immutableCast(getOrderInterfaces().subOrder(1, interfaces.size()).getSet());

        finalizeInit();
    }

    public Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Where where = Where.TRUE;
        for(Interface propertyInterface : interfaces)
            if(propertyInterface!= objectInterface)
                where = where.and(joinImplement.get(propertyInterface).getWhere());
        return joinImplement.get(objectInterface).and(where);
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        if(commonValue!=null)
            return MapFact.singleton((AndFormulaProperty.Interface) objectInterface, commonValue);
        return super.getInterfaceCommonClasses(commonValue);
    }

}
