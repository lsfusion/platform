package lsfusion.server.logics.property.derived;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImFilterValueMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.filter.ContextFilter;
import lsfusion.server.logics.form.interactive.instance.property.CalcPropertyObjectInstance;
import lsfusion.server.logics.form.interactive.InstanceFactory;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInterfaceInstance;
import lsfusion.server.logics.form.interactive.instance.filter.FilterInstance;
import lsfusion.server.logics.form.interactive.instance.filter.NotNullFilterInstance;
import lsfusion.server.logics.DataObject;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.logics.property.PullChangeProperty;
import lsfusion.server.logics.action.session.change.PropertyChanges;

// определяет не максимум изменения, а для конкретных входов
public class OnChangeProperty<T extends PropertyInterface,P extends PropertyInterface> extends PullChangeProperty<T, P, OnChangeProperty.Interface<T, P>> {

    public abstract static class Interface<T extends PropertyInterface, P extends PropertyInterface> extends PropertyInterface<Interface<T, P>> {

        Interface(int ID) {
            super(ID);
        }

        public abstract Expr getExpr();

        public abstract PropertyObjectInterfaceInstance getInterface(ImMap<T, DataObject> mapOnValues, ImMap<P, DataObject> mapToValues, ObjectInstance valueObject);
    }

    public static class KeyOnInterface<T extends PropertyInterface, P extends PropertyInterface> extends Interface<T, P> {
        T propertyInterface;

        public KeyOnInterface(T propertyInterface) {
            super(propertyInterface.ID);

            this.propertyInterface = propertyInterface;
        }

        public Expr getExpr() {
            return propertyInterface.getChangeExpr();
        }

        @Override
        public PropertyObjectInterfaceInstance getInterface(ImMap<T, DataObject> mapOnValues, ImMap<P, DataObject> mapToValues, ObjectInstance valueObject) {
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
            return propertyInterface.getChangeExpr();
        }

        @Override
        public PropertyObjectInterfaceInstance getInterface(ImMap<T, DataObject> mapOnValues, ImMap<P, DataObject> mapToValues, ObjectInstance valueObject) {
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
            return toChange.getChangeExpr();
        }

        @Override
        public PropertyObjectInterfaceInstance getInterface(ImMap<T, DataObject> mapOnValues, ImMap<P, DataObject> mapToValues, ObjectInstance valueObject) {
            return valueObject;
        }
    }

    public static <T extends PropertyInterface, P extends PropertyInterface> ImOrderSet<Interface<T, P>> getInterfaces(CalcProperty<T> onChange, CalcProperty<P> toChange) {
        return onChange.getFriendlyOrderInterfaces().mapOrderSetValues(new GetValue<Interface<T, P>, T>() {
            public Interface<T, P> getMapValue(T value) {
                return new KeyOnInterface<>(value);
            }
        }).addOrderExcl(toChange.getFriendlyOrderInterfaces().mapOrderSetValues(new GetValue<Interface<T, P>, P>() {
            public Interface<T, P> getMapValue(P value) {
                return new KeyToInterface<>(value);
            }
        })).addOrderExcl(new ValueInterface<T, P>(toChange));
    }

    public OnChangeProperty(CalcProperty<T> onChange, CalcProperty<P> toChange) {
        super(LocalizedString.concatList(onChange.caption, " по (", toChange.caption, ")"), getInterfaces(onChange, toChange), onChange, toChange);

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<Interface<T, P>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(!calcType.isExpr()) // пока так
            calcType = CalcType.EXPR;

        ImFilterValueMap<Interface<T, P>, Expr> mvMapExprs = interfaces.mapFilterValues();
        MExclMap<T, Expr> mOnChangeExprs = MapFact.mExclMapMax(interfaces.size());
        for(int i=0,size=interfaces.size();i<size;i++) {
            Interface<T, P> propertyInterface = interfaces.get(i);
            if(propertyInterface instanceof KeyOnInterface)
                mOnChangeExprs.exclAdd(((KeyOnInterface<T, P>) propertyInterface).propertyInterface, joinImplement.get(propertyInterface));
            else
                mvMapExprs.mapValue(i, propertyInterface.getExpr());
        }
        ImMap<Interface<T, P>, Expr> mapExprs = mvMapExprs.immutableValue();

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(mOnChangeExprs.immutable(),
                calcType, toChange.getChangeModifier(propChanges, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.LOGICAL(), joinImplement.filterIncl(mapExprs.keys())); // filters (constraints)
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return resultExpr;
    }

    public ContextFilter getContextFilter(final ImMap<T, DataObject> mapOnValues, final ImMap<P, DataObject> mapToValues, final ObjectEntity valueObject) {
        return new ContextFilter() {
            public FilterInstance getFilter(final InstanceFactory factory) {
                ImMap<Interface<T, P>, PropertyObjectInterfaceInstance> interfaceImplement = interfaces.mapValues(new GetValue<PropertyObjectInterfaceInstance, Interface<T, P>>() {
                    public PropertyObjectInterfaceInstance getMapValue(Interface<T, P> value) {
                        return value.getInterface(mapOnValues, mapToValues, valueObject.getInstance(factory));
                    }});
                return new NotNullFilterInstance<>(
                        new CalcPropertyObjectInstance<>(OnChangeProperty.this, interfaceImplement));
            }
        };

    }
}
