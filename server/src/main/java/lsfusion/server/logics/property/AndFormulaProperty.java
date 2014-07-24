package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetIndex;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.ServerResourceBundle;
import lsfusion.server.session.PropertyChanges;

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

    public AndFormulaProperty(int size) {
        super(ServerResourceBundle.getString("logics.property.if"), getInterfaces(size));
        objectInterface = (ObjectInterface) getOrderInterfaces().get(0);
        andInterfaces = BaseUtils.immutableCast(getOrderInterfaces().subOrder(1, interfaces.size()).getSet());

        finalizeInit();
    }

    public Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Where where = Where.TRUE;
        for(Interface propertyInterface : interfaces)
            if(propertyInterface!= objectInterface)
                where = where.and(joinImplement.get(propertyInterface).getWhere());
        return joinImplement.get(objectInterface).and(where);
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue, PrevClasses prevSameClasses) {
        if(commonValue!=null)
            return MapFact.singleton((AndFormulaProperty.Interface) objectInterface, commonValue);
        return super.getInterfaceCommonClasses(commonValue, prevSameClasses);
    }

}
