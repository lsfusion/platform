package platform.server.logics.property.derived;

import platform.base.QuickSet;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyObjectEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.logics.DataObject;
import platform.server.logics.property.ChangeProperty;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.*;

// св-во которое дает максимальное значение при изменении DataProperty для переданных ключей и значения
public class MaxChangeProperty<T extends PropertyInterface,P extends PropertyInterface> extends ChangeProperty<MaxChangeProperty.Interface<P>> {

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
        super(onChange.getSID() +"_CH_"+ toChange.getSID(),onChange.caption+" по ("+toChange.caption+")", getInterfaces(toChange));
        this.onChange = onChange;
        this.toChange = toChange;

        finalizeInit();
    }

    public static QuickSet<Property> getUsedChanges(Property<?> onChange, Property<?> toChange, StructChanges propChanges) {
        return QuickSet.add(toChange.getUsedDataChanges(propChanges), onChange.getUsedChanges(propChanges));
    }

    protected void fillDepends(Set<Property> depends, boolean derived) {
        depends.add(onChange);
        depends.add(toChange);
    }

    protected QuickSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return getUsedChanges(onChange,toChange, propChanges);
    }

    protected Expr calculateExpr(Map<Interface<P>, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        Map<Interface<P>, Expr> mapExprs = new HashMap<Interface<P>, Expr>();
        for(Interface<P> propertyInterface : interfaces)
            mapExprs.put(propertyInterface, propertyInterface.getExpr());

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(onChange.getMapKeys(),
                toChange.getChangeModifier(propChanges, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.ANY, joinImplement);
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
