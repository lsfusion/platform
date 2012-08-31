package platform.server.logics.property.derived;

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

// св-во которое дает максимальное значение при изменении DataProperty для переданных ключей и значения
public class MaxChangeProperty<T extends PropertyInterface,P extends PropertyInterface> extends PullChangeProperty<T, P, MaxChangeProperty.Interface<P>> {

    public abstract static class Interface<P extends PropertyInterface> extends PropertyInterface<Interface<P>> {

        Interface(int ID) {
            super(ID);
        }

        public abstract Expr getExpr();

        public abstract PropertyObjectInterfaceEntity getInterface(Map<P,DataObject> mapValues, ObjectEntity valueObject);
    }

    public static class KeyInterface<P extends PropertyInterface> extends Interface<P> {

        P propertyInterface;

        public KeyInterface(P propertyInterface) {
            super(propertyInterface.ID);

            this.propertyInterface = propertyInterface;
        }

        public Expr getExpr() {
            return propertyInterface.changeExpr;
        }

        public PropertyObjectInterfaceEntity getInterface(Map<P, DataObject> mapValues, ObjectEntity valueObject) {
            return mapValues.get(propertyInterface);
        }
    }

    public static class ValueInterface<P extends PropertyInterface> extends Interface<P> {

        CalcProperty<P> toChange;

        public ValueInterface(CalcProperty<P> toChange) {
            super(1000);

            this.toChange = toChange;  
        }

        public Expr getExpr() {
            return toChange.changeExpr;
        }

        public PropertyObjectInterfaceEntity getInterface(Map<P, DataObject> mapValues, ObjectEntity valueObject) {
            return valueObject;
        }
    }

    public static <P extends PropertyInterface> List<Interface<P>> getInterfaces(CalcProperty<P> property) {
        List<Interface<P>> result = new ArrayList<Interface<P>>();
        for(P propertyInterface : property.interfaces)
            result.add(new KeyInterface<P>(propertyInterface));
        result.add(new ValueInterface<P>(property));
        return result;
    }

    public MaxChangeProperty(CalcProperty<T> onChange, CalcProperty<P> toChange) {
        super(onChange.getSID() +"_CH_"+ toChange.getSID(),onChange.caption+" по ("+toChange.caption+")", getInterfaces(toChange), onChange, toChange);

        finalizeInit();
    }

    protected void fillDepends(Set<CalcProperty> depends, boolean events) {
        depends.add(onChange);
        depends.add(toChange);
    }

    protected Expr calculateExpr(Map<Interface<P>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(propClasses) // пока так
            propClasses = false;

        Map<Interface<P>, Expr> mapExprs = new HashMap<Interface<P>, Expr>();
        for(Interface<P> propertyInterface : interfaces)
            mapExprs.put(propertyInterface, propertyInterface.getExpr());

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(onChange.getMapKeys(),
                propClasses, toChange.getChangeModifier(propChanges, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.ANY, joinImplement);
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }

    public CalcPropertyObjectEntity<Interface<P>> getPropertyObjectEntity(Map<P, DataObject> mapValues, ObjectEntity valueObject) {
        Map<Interface<P>, PropertyObjectInterfaceEntity> interfaceImplement = new HashMap<Interface<P>, PropertyObjectInterfaceEntity>();
        for(Interface<P> propertyInterface : interfaces)
            interfaceImplement.put(propertyInterface, propertyInterface.getInterface(mapValues, valueObject));
        return new CalcPropertyObjectEntity<Interface<P>>(this,interfaceImplement);
    }
}
