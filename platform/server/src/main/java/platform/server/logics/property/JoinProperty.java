package platform.server.logics.property;

import platform.base.SFunctionSet;
import platform.base.col.MapFact;
import platform.base.col.SetFact;
import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderSet;
import platform.base.col.interfaces.immutable.ImSet;
import platform.base.col.interfaces.mutable.MSet;
import platform.base.col.interfaces.mutable.mapvalue.GetIndex;
import platform.base.col.interfaces.mutable.mapvalue.GetValue;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.form.ServerResponse;
import platform.server.caches.IdentityInstanceLazy;
import platform.server.caches.IdentityLazy;
import platform.server.classes.CustomClass;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.form.entity.FormEntity;
import platform.server.form.entity.ObjectEntity;
import platform.server.form.entity.PropertyDrawEntity;
import platform.server.form.entity.PropertyObjectInterfaceEntity;
import platform.server.form.entity.drilldown.DrillDownFormEntity;
import platform.server.form.entity.drilldown.JoinDrillDownFormEntity;
import platform.server.form.view.DefaultFormView;
import platform.server.form.view.PropertyDrawView;
import platform.server.logics.BusinessLogics;
import platform.server.logics.property.actions.edit.AggChangeActionProperty;
import platform.server.logics.property.derived.DerivedProperty;
import platform.server.session.DataChanges;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;
import platform.server.session.StructChanges;

import java.util.Iterator;
import java.util.Set;

import static platform.base.BaseUtils.capitalize;
import static platform.server.logics.ServerResourceBundle.getString;

public class JoinProperty<T extends PropertyInterface> extends SimpleIncrementProperty<JoinProperty.Interface> {
    public final CalcPropertyImplement<T, CalcPropertyInterfaceImplement<Interface>> implement;

    public static class Interface extends PropertyInterface<Interface> {
        Interface(int ID) {
            super(ID);
        }
    }

    public static GetIndex<Interface> genInterface = new GetIndex<Interface>() {
        public Interface getMapValue(int i) {
            return new Interface(i);
        }};
    public static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, genInterface);
    }

    private static <T extends PropertyInterface> boolean isIdentity(ImSet<Interface> interfaces, CalcPropertyImplement<T, CalcPropertyInterfaceImplement<Interface>> implement) {
        Set<Interface> rest = SetFact.mAddRemoveSet(interfaces);
        for(CalcPropertyInterfaceImplement<Interface> impl : implement.mapping.values())
            if(!(impl instanceof Interface && rest.remove((Interface)impl)))
                return false;
        return rest.isEmpty();
    }
    
    public JoinProperty(String sID, String caption, ImOrderSet<Interface> interfaces, boolean implementChange, CalcPropertyImplement<T, CalcPropertyInterfaceImplement<Interface>> implement) {
        super(sID, caption, interfaces);
        this.implement = implement;
        this.implementChange = implementChange || isIdentity(this.interfaces, implement);

        finalizeInit();
    }

    private ImMap<T, Expr> getJoinImplements(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getJoinImplements(joinImplement, false, propChanges, changedWhere);
    }

    private ImMap<T, Expr> getJoinImplements(final ImMap<Interface, ? extends Expr> joinImplement, final boolean propClasses, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        return implement.mapping.mapItValues(new GetValue<Expr, CalcPropertyInterfaceImplement<Interface>>() {
            public Expr getMapValue(CalcPropertyInterfaceImplement<Interface> value) {
                return value.mapExpr(joinImplement, propClasses, propChanges, changedWhere);
            }});
    }

    public Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return implement.property.getExpr(getJoinImplements(joinImplement, propClasses, propChanges, changedWhere), propClasses, propChanges, changedWhere);
    }

    @Override
    public void fillDepends(MSet<CalcProperty> depends, boolean events) {
        fillDepends(depends,implement.mapping.values());
        depends.add((CalcProperty) implement.property);
    }

    // разрешить менять основное свойство
    public final boolean implementChange;
    
    @Override
    protected ImSet<CalcProperty> calculateUsedDataChanges(StructChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            MSet<CalcProperty> mResult = SetFact.mSet();
            for(CalcProperty<?> property : getDepends()) {
                mResult.addAll(property.getUsedDataChanges(propChanges));
                mResult.addAll(property.getUsedChanges(propChanges));
            }
            return mResult.immutable();
        }

        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null) {
            MSet<CalcProperty> mImplementDepends = SetFact.mSet();
            implement.mapping.get(andInterface).mapFillDepends(mImplementDepends);
            return SetFact.add(propChanges.getUsedDataChanges(mImplementDepends.immutable()), getUsedChanges(propChanges));
        }

        CalcProperty<T> implementProperty = implement.property;
        if(implementChange) {
            MSet<CalcProperty> mImplementProps = SetFact.mSet();
            fillDepends(mImplementProps,implement.mapping.values());
            return SetFact.add(implementProperty.getUsedDataChanges(propChanges), propChanges.getUsedChanges(mImplementProps.immutable()));
        }
        if(implement.mapping.size() == 1 && implementProperty.aggProp) {
            // пока тупо MGProp'им назад
            return SetFact.add((((CalcPropertyMapImplement<?, Interface>) implement.mapping.singleValue()).property).getUsedDataChanges(propChanges), implementProperty.getUsedChanges(propChanges));
        }

        return super.calculateUsedDataChanges(propChanges);
    }

    // для Compare - data changes, тут чтобы не мусорить в Property
    private static DataChanges getCompareDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges, CalcPropertyInterfaceImplement<Interface> changeImp, CalcPropertyInterfaceImplement<Interface> valueImp) {
        ImMap<Interface, Expr> mapExprs = change.getMapExprs();
        Expr toChangeExpr = valueImp.mapExpr(mapExprs, propChanges);
        Where toChangeWhere = change.expr.getWhere();
        return changeImp.mapJoinDataChanges(mapExprs, toChangeExpr.and(toChangeWhere), // меняем на новое значение, если надо и скидываем в null если было какое-то
                change.where.and(toChangeWhere.or(toChangeExpr.compare(changeImp.mapExpr(mapExprs, propChanges), Compare.EQUALS))), changedWhere, propChanges);
    }
    
    // для And - data changes, тут чтобы не мусорить в Property
    private static <T extends PropertyInterface> T getObjectAndInterface(CalcProperty<T> property) {
        if(property instanceof AndFormulaProperty)
            return (T) ((AndFormulaProperty)property).objectInterface;
        if(property instanceof JoinProperty) {
            CalcPropertyImplement<PropertyInterface, CalcPropertyInterfaceImplement<Interface>> joinImplement = ((JoinProperty<PropertyInterface>) property).implement;
            PropertyInterface andInterface = getObjectAndInterface(joinImplement.property);
            if(andInterface!=null) {
                CalcPropertyInterfaceImplement<Interface> andJoinInterface = joinImplement.mapping.get(andInterface);
                if(andJoinInterface instanceof Interface)
                    return (T) andJoinInterface;
            }
        }
        return null;
    }
    
    private static <T extends PropertyInterface> Where getAndWhere(CalcProperty<T> property, final ImMap<T, ? extends Expr> mapExprs, final PropertyChanges propChanges) {
        if(property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)property;
            Where where = Where.TRUE;
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface)
                    where = where.and(mapExprs.get((T)andInterface).getWhere());
            return where;
        }
        if(property instanceof JoinProperty) {
            CalcPropertyImplement<PropertyInterface, CalcPropertyInterfaceImplement<Interface>> joinImplement = ((JoinProperty<PropertyInterface>) property).implement;
            ImMap<PropertyInterface, Expr> mapJoinExprs = joinImplement.mapping.mapValues(new GetValue<Expr, CalcPropertyInterfaceImplement<Interface>>() {
                public Expr getMapValue(CalcPropertyInterfaceImplement<Interface> value) {
                    return value.mapExpr((ImMap<Interface, ? extends Expr>) mapExprs, propChanges);
                }});
            return getAndWhere(joinImplement.property, mapJoinExprs, propChanges);
        }
        throw new RuntimeException("should not be");
    }

    @Override
    @IdentityLazy
    public ImSet<DataProperty> getChangeProps() {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) {
            assert implement.mapping.size()==2;
            return implement.mapping.getValue(0).mapChangeProps().merge(implement.mapping.getValue(1).mapChangeProps());
        }

        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null)
            return implement.mapping.get(andInterface).mapChangeProps();

        if(implementChange)
            return implement.property.getChangeProps();

        if(implement.mapping.size() == 1 && implement.property.aggProp)
            return implement.mapping.singleValue().mapChangeProps();

        return super.getChangeProps();
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<T> i = implement.property.interfaces.iterator();
            CalcPropertyInterfaceImplement<Interface> op1 = implement.mapping.get(i.next());
            CalcPropertyInterfaceImplement<Interface> op2 = implement.mapping.get(i.next());

            // сначала первый на второй пытаемся изменить, затем для оставшихся второй на первый второй
            WhereBuilder compareChangedWhere = new WhereBuilder();
            DataChanges result = getCompareDataChanges(change, compareChangedWhere, propChanges, op1, op2);
            if(changedWhere!=null) changedWhere.add(compareChangedWhere.toWhere());
            return result.add(getCompareDataChanges(change.and(compareChangedWhere.toWhere().not()), changedWhere, propChanges, op2, op1));
        }

        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null) {
            ImMap<Interface, Expr> mapExprs = change.getMapExprs();
            return implement.mapping.get(andInterface).mapJoinDataChanges(mapExprs, change.expr,
                    change.where.and(getAndWhere(this, mapExprs, propChanges)), changedWhere, propChanges);
        }

        if(implementChange) // groupBy'им выбирая max
            return implement.property.getJoinDataChanges(getJoinImplements(change.getMapExprs(), propChanges, null), change.expr, change.where, propChanges, changedWhere); //.map(mapInterfaces)

        if(implement.mapping.size() == 1 && implement.property.aggProp) {
            // пока тупо MGProp'им назад
            CalcPropertyMapImplement<?, Interface> implementSingle = (CalcPropertyMapImplement<?, Interface>) implement.mapping.singleValue();
            KeyExpr keyExpr = new KeyExpr("key");
            Expr groupExpr = GroupExpr.create(MapFact.singleton(0, implement.property.getExpr(MapFact.singleton(implement.property.interfaces.single(), keyExpr), propChanges)),
                    keyExpr, keyExpr.isUpClass(implementSingle.property.getValueClass()), GroupType.ANY, MapFact.singleton(0, change.expr));
            return implementSingle.mapDataChanges(
                    new PropertyChange<Interface>(change, groupExpr), changedWhere, propChanges);
        }

        return super.calculateDataChanges(change, changedWhere, propChanges);
    }

    @Override
    @IdentityInstanceLazy
    public ActionPropertyMapImplement<?, Interface> getDefaultEditAction(String editActionSID, CalcProperty filterProperty) {
        CalcProperty<T> aggProp = implement.property;

        if (aggProp instanceof AndFormulaProperty) {
            final AndFormulaProperty andProperty = (AndFormulaProperty) aggProp;
            ImCol<CalcPropertyInterfaceImplement<Interface>> ands = implement.mapping.filterFn(new SFunctionSet<T>() {
                public boolean contains(T element) {
                    return element != andProperty.objectInterface;
                }
            }).values();
            ActionPropertyMapImplement<?, Interface> implementEdit = implement.mapping.get((T) andProperty.objectInterface).mapEditAction(editActionSID, filterProperty);
            if (implementEdit != null) {
                return DerivedProperty.createIfAction(
                        interfaces,
                        DerivedProperty.createAnd(interfaces, DerivedProperty.<Interface>createTrue(), ands),
                        implementEdit,
                        null
                );
            }
        }

        if (implement.mapping.size() == 1 && !implementChange) {
            if (editActionSID.equals(ServerResponse.CHANGE_WYS)) {
                ActionPropertyMapImplement<?, Interface> changeActionImplement = getEditAction(ServerResponse.CHANGE);
                ValueClass aggClass = ((CalcPropertyMapImplement<?, Interface>) implement.mapping.singleValue()).property.getValueClass();

                ImOrderSet<Interface> listInterfaces = getOrderInterfaces();
                AggChangeActionProperty<T> aggChangeActionProperty =
                        new AggChangeActionProperty<T>("AGGCH" + getSID(), "sys", listInterfaces, aggProp, aggClass, changeActionImplement);
                return aggChangeActionProperty.getImplement(listInterfaces);
            } else {
                // тут вообще надо что=то типа с join'ить (assertion что filterProperty с одним интерфейсом)
                return implement.mapping.singleValue().mapEditAction(editActionSID, aggProp);
            }
        }
        return super.getDefaultEditAction(editActionSID, filterProperty);
    }

    public boolean checkEquals() {
        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<Interface>> andImplement
                    = (CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<Interface>>) implement;

            CalcPropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof CalcPropertyMapImplement) {
                return ((CalcPropertyMapImplement) objectIface).property.checkEquals();
            }
        }

        return implement.property.checkEquals();
    }

    @Override
    public void proceedDefaultDraw(PropertyDrawEntity<Interface> entity, FormEntity<?> form) {
        super.proceedDefaultDraw(entity, form);
        if (implement.mapping.size() == 1 &&
                (implement.mapping.singleValue() instanceof CalcPropertyMapImplement) &&
                ((CalcPropertyMapImplement<?, Interface>) implement.mapping.singleValue()).property instanceof ObjectClassProperty) {
            PropertyObjectInterfaceEntity mapObject = entity.propertyObject.mapping.singleValue();
            if (mapObject instanceof ObjectEntity && !((CustomClass) ((ObjectEntity) mapObject).baseClass).hasChildren())
                entity.forceViewType = ClassViewType.HIDE;
        }
    }

    @Override
    public void proceedDefaultDesign(PropertyDrawView propertyView, DefaultFormView view) {
        super.proceedDefaultDesign(propertyView, view);

        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<Interface>> andImplement
                    = (CalcPropertyImplement<AndFormulaProperty.Interface, CalcPropertyInterfaceImplement<Interface>>) implement;

            CalcPropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof CalcPropertyMapImplement) {
                ((CalcPropertyMapImplement) objectIface).property.proceedDefaultDesign(propertyView, view);
            }
        }
    }

    @Override
    public ImMap<Interface, ValueClass> getInterfaceCommonClasses(ValueClass commonValue) {
        ImMap<Interface, ValueClass> result = super.getInterfaceCommonClasses(commonValue);
        ImMap<T, ValueClass> commonClasses = implement.property.getInterfaceCommonClasses(commonValue);
        for(int i=0,size=implement.mapping.size();i<size;i++)
            result = or(interfaces, result, implement.mapping.getValue(i).mapInterfaceCommonClasses(commonClasses.get(implement.mapping.getKey(i))));
        return result;
    }

    @Override
    public boolean supportsDrillDown() {
        return isFull() && implement.property.isFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(BusinessLogics BL) {
        return new JoinDrillDownFormEntity(
                "drillDown" + capitalize(getSID()) + "Form",
                getString("logics.property.drilldown.form.join"), this, BL
        );
    }
}
