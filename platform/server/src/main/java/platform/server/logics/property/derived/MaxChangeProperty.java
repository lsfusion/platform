package platform.server.logics.property.derived;

import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.DataObject;
import platform.server.logics.property.AggregateProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// св-во которое дает максимальное значение при изменении DataProperty для переданных ключей и значения
public class MaxChangeProperty<T extends PropertyInterface,P extends PropertyInterface> extends AggregateProperty<MaxChangeProperty.Interface<P>> {

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

        Property<P> toChange;

        public ValueInterface(Property<P> toChange) {
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

    // assert что constraint.isFalse
    final Property<T> onChange;
    final Property<P> toChange;

    public static <P extends PropertyInterface> List<Interface<P>> getInterfaces(Property<P> property) {
        List<Interface<P>> result = new ArrayList<Interface<P>>();
        for(P propertyInterface : property.interfaces)
            result.add(new KeyInterface<P>(propertyInterface));
        result.add(new ValueInterface<P>(property));
        return result;
    }

    public MaxChangeProperty(Property<T> onChange, Property<P> toChange) {
        super(onChange.sID+"_CH_"+toChange.sID,onChange.caption+" по ("+toChange.caption+")", getInterfaces(toChange));
        this.onChange = onChange;
        this.toChange = toChange;
    }

    public static <U extends Changes<U>> U getUsedChanges(Property<?> onChange, Property<?> toChange, Modifier<U> modifier) {
        return modifier.newChanges().addChanges(onChange.getUsedChanges(toChange.getChangeModifier(modifier, false)));
    }

    protected <U extends Changes<U>> U calculateUsedChanges(Modifier<U> modifier) {
        return getUsedChanges(onChange,toChange,modifier);
    }

    protected Expr calculateExpr(Map<Interface<P>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<Interface<P>, Expr> mapExprs = new HashMap<Interface<P>, Expr>();
        for(Interface<P> propertyInterface : interfaces)
            mapExprs.put(propertyInterface, propertyInterface.getExpr());

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(onChange.getMapKeys(),
                toChange.getChangeModifier(modifier, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.MAX, joinImplement);
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }

    public PropertyObjectEntity<Interface<P>> getPropertyObjectEntity(Map<P, DataObject> mapValues, ObjectEntity valueObject) {
        Map<Interface<P>, PropertyObjectInterfaceEntity> interfaceImplement = new HashMap<Interface<P>, PropertyObjectInterfaceEntity>();
        for(Interface<P> propertyInterface : interfaces)
            interfaceImplement.put(propertyInterface, propertyInterface.getInterface(mapValues, valueObject));
        return new PropertyObjectEntity<Interface<P>>(this,interfaceImplement);
    }
}
