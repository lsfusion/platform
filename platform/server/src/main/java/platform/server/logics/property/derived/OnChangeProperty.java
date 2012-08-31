package platform.server.logics.property.derived;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.CalcPropertyObjectEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.DataObject;
import platform.server.logics.property.CalcProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PullChangeProperty;
import platform.server.session.PropertyChanges;

import java.util.*;

// определяет не максимум изменения, а для конкретных входов
public class OnChangeProperty<T extends PropertyInterface,P extends PropertyInterface> extends PullChangeProperty<T, P, OnChangeProperty.Interface<T, P>> {

    public abstract static class Interface<T extends PropertyInterface, P extends PropertyInterface> extends PropertyInterface<Interface<T, P>> {

        Interface(int ID) {
            super(ID);
        }

        public abstract Expr getExpr();

        public abstract PropertyObjectInterfaceEntity getInterface(Map<T, DataObject> mapOnValues, Map<P,DataObject> mapToValues, ObjectEntity valueObject);
    }

    public static class KeyOnInterface<T extends PropertyInterface, P extends PropertyInterface> extends Interface<T, P> {
        T propertyInterface;

        public KeyOnInterface(T propertyInterface) {
            super(propertyInterface.ID);

            this.propertyInterface = propertyInterface;
        }

        public Expr getExpr() {
            return propertyInterface.changeExpr;
        }

        @Override
        public PropertyObjectInterfaceEntity getInterface(Map<T, DataObject> mapOnValues, Map<P, DataObject> mapToValues, ObjectEntity valueObject) {
            return mapOnValues.get(propertyInterface);
        }
    }

    public static class KeyToInterface<T extends PropertyInterface, P extends PropertyInterface> extends Interface<T, P> {

        P propertyInterface;

        public KeyToInterface(P propertyInterface) {
            super(propertyInterface.ID);

            this.propertyInterface = propertyInterface;
        }

        public Expr getExpr() {
            return propertyInterface.changeExpr;
        }

        @Override
        public PropertyObjectInterfaceEntity getInterface(Map<T, DataObject> mapOnValues, Map<P, DataObject> mapToValues, ObjectEntity valueObject) {
            return mapToValues.get(propertyInterface);
        }
    }

    public static class ValueInterface<T extends PropertyInterface, P extends PropertyInterface> extends Interface<T, P> {

        CalcProperty<P> toChange;

        public ValueInterface(CalcProperty<P> toChange) {
            super(1000);

            this.toChange = toChange;
        }

        public Expr getExpr() {
            return toChange.changeExpr;
        }

        @Override
        public PropertyObjectInterfaceEntity getInterface(Map<T, DataObject> mapOnValues, Map<P, DataObject> mapToValues, ObjectEntity valueObject) {
            return valueObject;
        }
    }

    public static <T extends PropertyInterface, P extends PropertyInterface> List<Interface<T, P>> getInterfaces(CalcProperty<T> onChange, CalcProperty<P> toChange) {
        List<Interface<T, P>> result = new ArrayList<Interface<T, P>>();
        for(T propertyInterface : onChange.interfaces)
            result.add(new KeyOnInterface<T, P>(propertyInterface));
        for(P propertyInterface : toChange.interfaces)
            result.add(new KeyToInterface<T, P>(propertyInterface));
        result.add(new ValueInterface<T, P>(toChange));
        return result;
    }

    public OnChangeProperty(CalcProperty<T> onChange, CalcProperty<P> toChange) {
        super(onChange.getSID() +"_ONCH_"+ toChange.getSID(),onChange.caption+" по ("+toChange.caption+")", getInterfaces(onChange, toChange), onChange, toChange);

        finalizeInit();
    }

    @Override
    protected void fillDepends(Set<CalcProperty> depends, boolean events) {
        depends.add(onChange);
        depends.add(toChange);
    }

    protected Expr calculateExpr(Map<Interface<T, P>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(propClasses) // пока так
            propClasses = false;

        Map<Interface<T, P>, Expr> mapExprs = new HashMap<Interface<T, P>, Expr>();
        Map<T, Expr> onChangeExprs = new HashMap<T, Expr>();
        for(Interface<T, P> propertyInterface : interfaces)
            if(propertyInterface instanceof KeyOnInterface)
                onChangeExprs.put(((KeyOnInterface<T,P>)propertyInterface).propertyInterface, joinImplement.get(propertyInterface));
            else
                mapExprs.put(propertyInterface, propertyInterface.getExpr());

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(onChangeExprs,
                propClasses, toChange.getChangeModifier(propChanges, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.ANY, BaseUtils.filterKeys(joinImplement, mapExprs.keySet()));
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }

    public CalcPropertyObjectEntity<Interface<T, P>> getPropertyObjectEntity(Map<T, DataObject> mapOnValues, Map<P, DataObject> mapToValues, ObjectEntity valueObject) {
        Map<Interface<T, P>, PropertyObjectInterfaceEntity> interfaceImplement = new HashMap<Interface<T, P>, PropertyObjectInterfaceEntity>();
        for(Interface<T, P> propertyInterface : interfaces)
            interfaceImplement.put(propertyInterface, propertyInterface.getInterface(mapOnValues, mapToValues, valueObject));
        return new CalcPropertyObjectEntity<Interface<T, P>>(this,interfaceImplement);
    }
}
