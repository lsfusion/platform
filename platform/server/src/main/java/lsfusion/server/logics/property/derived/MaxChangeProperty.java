package lsfusion.server.logics.property.derived;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.form.entity.CalcPropertyObjectEntity;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.PropertyObjectInterfaceEntity;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.session.PropertyChanges;

// св-во которое дает максимальное значение при изменении DataProperty для переданных ключей и значения
public class MaxChangeProperty<T extends PropertyInterface,P extends PropertyInterface> extends PullChangeProperty<T, P, MaxChangeProperty.Interface<P>> {

    public abstract static class Interface<P extends PropertyInterface> extends PropertyInterface<Interface<P>> {

        Interface(int ID) {
            super(ID);
        }

        public abstract Expr getExpr();

        public abstract PropertyObjectInterfaceEntity getInterface(ImMap<P, DataObject> mapValues, ObjectEntity valueObject);
    }

    public static class KeyInterface<P extends PropertyInterface> extends Interface<P> {

        P propertyInterface;

        public KeyInterface(P propertyInterface) {
            super(propertyInterface.ID);

            this.propertyInterface = propertyInterface;
        }

        public Expr getExpr() {
            return propertyInterface.getChangeExpr();
        }

        public PropertyObjectInterfaceEntity getInterface(ImMap<P, DataObject> mapValues, ObjectEntity valueObject) {
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
            return toChange.getChangeExpr();
        }

        public PropertyObjectInterfaceEntity getInterface(ImMap<P, DataObject> mapValues, ObjectEntity valueObject) {
            return valueObject;
        }
    }

    public static <P extends PropertyInterface> ImOrderSet<Interface<P>> getInterfaces(CalcProperty<P> property) {
        return property.getOrderInterfaces().mapOrderSetValues(new GetValue<Interface<P>, P>() {
            public Interface<P> getMapValue(P value) {
                return new KeyInterface<P>(value);
            }
        }).addOrderExcl(new ValueInterface<P>(property));
    }

    public MaxChangeProperty(CalcProperty<T> onChange, CalcProperty<P> toChange) {
        super(onChange.getSID() +"_CH_"+ toChange.getSID(),onChange.caption+" по ("+toChange.caption+")", getInterfaces(toChange), onChange, toChange);

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<Interface<P>, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(propClasses) // пока так
            propClasses = false;

        ImMap<Interface<P>, Expr> mapExprs = interfaces.mapValues(new GetValue<Expr, Interface<P>>() {
            public Expr getMapValue(Interface<P> value) {
                return value.getExpr();
            }});

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(onChange.getMapKeys(),
                propClasses, toChange.getChangeModifier(propChanges, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.ANY, joinImplement);
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }

    public CalcPropertyObjectEntity<Interface<P>> getPropertyObjectEntity(final ImMap<P, DataObject> mapValues, final ObjectEntity valueObject) {
        ImMap<Interface<P>, PropertyObjectInterfaceEntity> interfaceImplement = interfaces.mapValues(new GetValue<PropertyObjectInterfaceEntity, Interface<P>>() {
            public PropertyObjectInterfaceEntity getMapValue(Interface<P> value) {
                return value.getInterface(mapValues, valueObject);
            }});
        return new CalcPropertyObjectEntity<Interface<P>>(this,interfaceImplement);
    }
}
