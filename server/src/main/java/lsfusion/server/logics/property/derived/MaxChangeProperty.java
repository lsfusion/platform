package lsfusion.server.logics.property.derived;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.form.entity.ObjectEntity;
import lsfusion.server.form.entity.filter.ContextFilter;
import lsfusion.server.form.instance.CalcPropertyObjectInstance;
import lsfusion.server.form.instance.InstanceFactory;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.filter.FilterInstance;
import lsfusion.server.form.instance.filter.NotFilterInstance;
import lsfusion.server.form.instance.filter.NotNullFilterInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcType;
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

        public abstract PropertyObjectInterfaceInstance getInterface(ImMap<P, DataObject> mapValues, ObjectInstance valueObject);
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

        public PropertyObjectInterfaceInstance getInterface(ImMap<P, DataObject> mapValues, ObjectInstance valueObject) {
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

        public PropertyObjectInterfaceInstance getInterface(ImMap<P, DataObject> mapValues, ObjectInstance valueObject) {
            return valueObject;
        }
    }

    public static <P extends PropertyInterface> ImOrderSet<Interface<P>> getInterfaces(CalcProperty<P> property) {
        return property.getFriendlyOrderInterfaces().mapOrderSetValues(new GetValue<Interface<P>, P>() {
            public Interface<P> getMapValue(P value) {
                return new KeyInterface<>(value);
            }
        }).addOrderExcl(new ValueInterface<>(property));
    }

    public MaxChangeProperty(CalcProperty<T> onChange, CalcProperty<P> toChange) {
        super(LocalizedString.concatList(onChange.caption, " по (", toChange.caption, ")"), getInterfaces(toChange), onChange, toChange);

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<Interface<P>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(!calcType.isExpr()) // пока так
            calcType = CalcType.EXPR;

        ImMap<Interface<P>, Expr> mapExprs = interfaces.mapValues(new GetValue<Expr, Interface<P>>() {
            public Expr getMapValue(Interface<P> value) {
                return value.getExpr();
            }});

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(onChange.getMapKeys(),
                calcType, toChange.getChangeModifier(propChanges, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.LOGICAL(), joinImplement); // constraints (filters)
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }

    public ContextFilter getContextFilter(final ImMap<P, DataObject> mapValues, final ObjectEntity valueObject) {
        return new ContextFilter() {
            public FilterInstance getFilter(final InstanceFactory factory) {
                ImMap<Interface<P>, PropertyObjectInterfaceInstance> interfaceImplement = interfaces.mapValues(new GetValue<PropertyObjectInterfaceInstance, Interface<P>>() {
                    public PropertyObjectInterfaceInstance getMapValue(Interface<P> value) {
                        return value.getInterface(mapValues, valueObject.getInstance(factory));
                    }});
                return new NotFilterInstance(new NotNullFilterInstance<>(
                        new CalcPropertyObjectInstance<>(MaxChangeProperty.this, interfaceImplement)));
            }
        };
    }
}
