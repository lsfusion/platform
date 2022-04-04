package lsfusion.server.logics.property;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.base.caches.IdentityStartLazy;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.StaticParamNullableExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.*;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.interactive.action.input.InputListEntity;
import lsfusion.server.logics.property.classes.data.AndFormulaProperty;
import lsfusion.server.logics.property.classes.data.CompareFormulaProperty;
import lsfusion.server.logics.property.classes.data.NotFormulaProperty;
import lsfusion.server.logics.property.classes.infer.*;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.JoinDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.function.IntFunction;

public class JoinProperty<T extends PropertyInterface> extends SimpleIncrementProperty<JoinProperty.Interface> {
    public final PropertyImplement<T, PropertyInterfaceImplement<Interface>> implement;

    public static class Interface extends PropertyInterface<Interface> {
        Interface(int ID) {
            super(ID);
        }
    }

    public static IntFunction<Interface> genInterface = Interface::new;
    public static ImOrderSet<Interface> getInterfaces(int intNum) {
        return SetFact.toOrderExclSet(intNum, genInterface);
    }

    public static <T extends PropertyInterface> boolean isIdentity(ImSet<Interface> interfaces, PropertyImplement<T, PropertyInterfaceImplement<Interface>> implement) {
        Set<Interface> rest = SetFact.mAddRemoveSet(interfaces);
        for(PropertyInterfaceImplement<Interface> impl : implement.mapping.values())
            if(!(impl instanceof Interface && rest.remove(impl)))
                return false;
        return rest.isEmpty();
    }
    public boolean isIdentity() {
        return isIdentity(this.interfaces, implement);
    }

    @Override
    public <X extends PropertyInterface> boolean isChangedWhen(boolean toNull, Property<X> changeProperty, ImRevMap<Interface, X> changeMapping) {
        if(isIdentity) {
            ImRevMap<T, Interface> joinMapping = BaseUtils.immutableCast(implement.mapping.toRevExclMap());
            return implement.property.isChangedWhen(toNull, changeProperty, joinMapping.join(changeMapping));
        }

        if(toNull && implement.property.isNotNull(AlgType.actionType))
            for(PropertyInterfaceImplement<Interface> mapImpl : implement.mapping.valueIt()) {
                if(mapImpl.mapChangedWhen(toNull, changeProperty, changeMapping))
                    return true;
            }

        return super.isChangedWhen(toNull, changeProperty, changeMapping);
    }

    public <X extends PropertyInterface> PropertyMapImplement<?, X> getIdentityImplement(ImRevMap<Interface, X> mapping) {
        if(isIdentity) {
            ImRevMap<T, Interface> joinMapping = BaseUtils.immutableCast(implement.mapping.toRevExclMap());
            return implement.property.getIdentityImplement(joinMapping.join(mapping));
        }
        return super.getIdentityImplement(mapping);
    }

    public JoinProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, PropertyImplement<T, PropertyInterfaceImplement<Interface>> implement) {
        this(caption, interfaces, false, implement);
    }
    public JoinProperty(LocalizedString caption, ImOrderSet<Interface> interfaces, boolean user, PropertyImplement<T, PropertyInterfaceImplement<Interface>> implement) {
        super(caption, interfaces);
        this.implement = implement;
        this.user = user;
        this.isIdentity = isIdentity();

        finalizeInit();
    }

    private ImMap<T, Expr> getJoinImplements(ImMap<Interface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return getJoinImplements(joinImplement, CalcType.EXPR, propChanges, changedWhere);
    }

    private ImMap<T, Expr> getJoinImplements(final ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges, final WhereBuilder changedWhere) {
        return implement.mapping.mapItValues(value -> value.mapExpr(joinImplement, calcType, propChanges, changedWhere));
    }
    
    public static <P extends PropertyInterface> boolean checkPrereadNull(ImMap<P, ? extends Expr> joinImplement, boolean notNull, ImCol<PropertyInterfaceImplement<P>> col, final CalcType calcType, final PropertyChanges propChanges) {
        if(!notNull || !calcType.isExpr())
            return false;

        // for prereads
        ImCol<PropertyInterfaceImplement<P>> complexMapping = col.filterCol(implement -> implement.mapHasPreread(propChanges));
        if(!complexMapping.isEmpty()) {
            // сортируем по сложности
            for(PropertyInterfaceImplement<P> mapImpl : complexMapping.sort(Comparator.comparingLong(PropertyInterfaceImplement::mapSimpleComplexity))) {
                WhereBuilder changedWhere = new WhereBuilder();
                if (mapImpl.mapExpr(joinImplement, calcType, propChanges, changedWhere).isNull() && changedWhere.toWhere().isFalse())
                    return true;
            }
        }

        return false;                
    }

    private boolean checkPrereadNull(ImMap<Interface, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges) {
        return checkPrereadNull(joinImplement, implement.property.isNotNull(calcType.getAlgInfo()), implement.mapping.values(), calcType, propChanges);
    }
    
    public Expr calculateExpr(ImMap<Interface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(checkPrereadNull(joinImplement, calcType, propChanges))
            return Expr.NULL();
        
        return implement.property.getExpr(getJoinImplements(joinImplement, calcType, propChanges, changedWhere), calcType, propChanges, changedWhere);
    }

    @Override
    public void fillDepends(MSet<Property> depends, boolean events) {
        fillDepends(depends,implement.mapping.values());
        depends.add(implement.property);
    }

    // разрешить менять основное свойство
    public final boolean isIdentity;
    
    private final boolean user;
    
    @Override
    protected ImSet<Property> calculateUsedDataChanges(StructChanges propChanges, CalcDataType type) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            MSet<Property> mResult = SetFact.mSet();
            for(Property<?> property : getDepends()) {
                mResult.addAll(property.getUsedDataChanges(propChanges, type));
                mResult.addAll(property.getUsedChanges(propChanges));
            }
            return mResult.immutable();
        }

        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null) {
            MSet<Property> mImplementDepends = SetFact.mSet();
            implement.mapping.get(andInterface).mapFillDepends(mImplementDepends);
            return SetFact.add(propChanges.getUsedDataChanges(type, mImplementDepends.immutable()), getUsedChanges(propChanges));
        }

        Property<T> implementProperty = implement.property;
        if(isIdentity) {
            MSet<Property> mImplementProps = SetFact.mSet();
            fillDepends(mImplementProps,implement.mapping.values());
            return SetFact.add(implementProperty.getUsedDataChanges(propChanges, type), propChanges.getUsedChanges(mImplementProps.immutable()));
        }

        return super.calculateUsedDataChanges(propChanges, type);
    }

    // для Compare - data changes, тут чтобы не мусорить в Property
    private static DataChanges getCompareDataChanges(PropertyChange<Interface> change, CalcDataType type, WhereBuilder changedWhere, PropertyChanges propChanges, PropertyInterfaceImplement<Interface> changeImp, PropertyInterfaceImplement<Interface> valueImp) {
        ImMap<Interface, Expr> mapExprs = change.getMapExprs();
        Expr toChangeExpr = valueImp.mapExpr(mapExprs, propChanges);
        Where toChangeWhere = change.expr.getWhere();
        return changeImp.mapJoinDataChanges(mapExprs, toChangeExpr.and(toChangeWhere), // меняем на новое значение, если надо и скидываем в null если было какое-то
                change.where.and(toChangeWhere.or(toChangeExpr.compare(changeImp.mapExpr(mapExprs, propChanges), Compare.EQUALS))), GroupType.ASSERTSINGLE_CHANGE(), changedWhere, propChanges, type);
    }
    
    // для And - data changes, тут чтобы не мусорить в Property
    private static <T extends PropertyInterface> T getObjectAndInterface(Property<T> property) {
        if(property instanceof AndFormulaProperty)
            return (T) ((AndFormulaProperty)property).objectInterface;
        if(property instanceof JoinProperty) {
            PropertyImplement<PropertyInterface, PropertyInterfaceImplement<Interface>> joinImplement = ((JoinProperty<PropertyInterface>) property).implement;
            PropertyInterface andInterface = getObjectAndInterface(joinImplement.property);
            if(andInterface!=null) {
                PropertyInterfaceImplement<Interface> andJoinInterface = joinImplement.mapping.get(andInterface);
                if(andJoinInterface instanceof Interface)
                    return (T) andJoinInterface;
            }
        }
        return null;
    }
    
    private static <T extends PropertyInterface> Where getAndWhere(Property<T> property, final ImMap<T, ? extends Expr> mapExprs, final PropertyChanges propChanges) {
        if(property instanceof AndFormulaProperty) {
            AndFormulaProperty andProperty = (AndFormulaProperty)property;
            Where where = Where.TRUE();
            for(AndFormulaProperty.Interface andInterface : andProperty.interfaces)
                if(andInterface != andProperty.objectInterface)
                    where = where.and(mapExprs.get((T)andInterface).getWhere());
            return where;
        }
        if(property instanceof JoinProperty) {
            PropertyImplement<PropertyInterface, PropertyInterfaceImplement<Interface>> joinImplement = ((JoinProperty<PropertyInterface>) property).implement;
            ImMap<PropertyInterface, Expr> mapJoinExprs = joinImplement.mapping.mapValues(value -> value.mapExpr((ImMap<Interface, ? extends Expr>) mapExprs, propChanges));
            return getAndWhere(joinImplement.property, mapJoinExprs, propChanges);
        }
        throw new RuntimeException("should not be");
    }

    @Override
    @IdentityStartLazy // только компиляция, построение лексикографики и несколько мелких использований
    public ImSet<DataProperty> getChangeProps() {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) {
            assert implement.mapping.size()==2;
            return implement.mapping.getValue(0).mapChangeProps().merge(implement.mapping.getValue(1).mapChangeProps());
        }

        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null)
            return implement.mapping.get(andInterface).mapChangeProps();

        if(isIdentity)
            return implement.property.getChangeProps();

        return super.getChangeProps();
    }

    @Override
    public boolean canBeHeurChanged(boolean global) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<T> i = implement.property.interfaces.iterator();
            PropertyInterfaceImplement<Interface> op1 = implement.mapping.get(i.next());
            PropertyInterfaceImplement<Interface> op2 = implement.mapping.get(i.next());

            // сначала первый на второй пытаемся изменить, затем для оставшихся второй на первый второй
            return (op1 instanceof PropertyMapImplement && ((PropertyMapImplement) op1).property.canBeHeurChanged(global)) || 
                    (op2 instanceof PropertyMapImplement && ((PropertyMapImplement) op2).property.canBeHeurChanged(global));
        }
        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null) {
            PropertyInterfaceImplement<Interface> andImplement = implement.mapping.get(andInterface);
            return andImplement instanceof PropertyMapImplement && ((PropertyMapImplement) andImplement).property.canBeHeurChanged(global);
        }
        if(isIdentity) // groupBy'им выбирая max
            return implement.property.canBeHeurChanged(global); // пока implementChange = identity
        return false;
    }

    @Override
    protected DataChanges calculateDataChanges(PropertyChange<Interface> change, CalcDataType type, WhereBuilder changedWhere, PropertyChanges propChanges) {
        if(implement.property instanceof CompareFormulaProperty && ((CompareFormulaProperty)implement.property).compare == Compare.EQUALS) { // если =
            assert implement.mapping.size()==2;
            Iterator<T> i = implement.property.interfaces.iterator();
            PropertyInterfaceImplement<Interface> op1 = implement.mapping.get(i.next());
            PropertyInterfaceImplement<Interface> op2 = implement.mapping.get(i.next());

            // сначала первый на второй пытаемся изменить, затем для оставшихся второй на первый второй
            WhereBuilder compareChangedWhere = new WhereBuilder();
            DataChanges result = getCompareDataChanges(change, type, compareChangedWhere, propChanges, op1, op2);
            if(changedWhere!=null) changedWhere.add(compareChangedWhere.toWhere());
            return result.add(getCompareDataChanges(change.and(compareChangedWhere.toWhere().not()), type, changedWhere, propChanges, op2, op1));
        }

        T andInterface = getObjectAndInterface(implement.property);
        if(andInterface!=null) {
            ImMap<Interface, Expr> mapExprs = change.getMapExprs();
            return implement.mapping.get(andInterface).mapJoinDataChanges(mapExprs, change.expr,
                    change.where.and(getAndWhere(this, mapExprs, propChanges)), GroupType.ASSERTSINGLE_CHANGE(), changedWhere, propChanges, type);
        }

        if(isIdentity) // groupBy'им выбирая max
            return implement.property.getJoinDataChanges(getJoinImplements(change.getMapExprs(), propChanges, null), change.expr, change.where, GroupType.ASSERTSINGLE_CHANGE(), propChanges, type, changedWhere); // пока implementChange = identity
        
        return super.calculateDataChanges(change, type, changedWhere, propChanges);
    }

    @Override
    @IdentityStrongLazy // STRONG пришлось поставить из-за использования в политике безопасности
    public ActionMapImplement<?, Interface> getDefaultEventAction(String eventActionSID, FormSessionScope defaultChangeEventScope, ImList<Property> viewProperties, String customChangeFunction) {
        Property<T> aggProp = implement.property;

        if (aggProp instanceof AndFormulaProperty) {
            final AndFormulaProperty andProperty = (AndFormulaProperty) aggProp;
            ImCol<PropertyInterfaceImplement<Interface>> ands = implement.mapping.filterFn(element -> element != andProperty.objectInterface).values();
            ActionMapImplement<?, Interface> implementEdit = implement.mapping.get((T) andProperty.objectInterface).mapEventAction(eventActionSID, defaultChangeEventScope, viewProperties, customChangeFunction);
            if (implementEdit != null) {
                return PropertyFact.createIfAction(
                        interfaces,
                        PropertyFact.createAnd(interfaces, PropertyFact.createTrue(), ands),
                        implementEdit,
                        null
                );
            }
        }

        if(eventActionSID.equals(ServerResponse.EDIT_OBJECT)) {
            ValueClass editClass = getValueClass(ClassType.tryEditPolicy);
            if((editClass != null && editClass.getDefaultOpenAction(getBaseLM()) != null)) // just call open action (see super implementation)
                return super.getDefaultEventAction(eventActionSID, defaultChangeEventScope, viewProperties, customChangeFunction);

            ActionMapImplement<?, T> editImplement = implement.property.getEventAction(eventActionSID, defaultChangeEventScope, ListFact.EMPTY(), customChangeFunction);
            if(editImplement != null)
                return PropertyFact.createJoinAction(editImplement.map(implement.mapping));
        }

        if (implement.mapping.size() == 1 && !isIdentity)
            return implement.mapping.singleValue().mapEventAction(eventActionSID, defaultChangeEventScope, viewProperties.addList(aggProp), customChangeFunction);

        return super.getDefaultEventAction(eventActionSID, defaultChangeEventScope, viewProperties, customChangeFunction);
    }

    public boolean checkEquals() {
        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>> andImplement
                    = (PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>>) implement;

            PropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof PropertyMapImplement) {
                return ((PropertyMapImplement) objectIface).property.checkEquals();
            }
        }

        return implement.property.checkEquals();
    }

    @Override // см. базовый метод
    public ImList<Property> getAndProperties() {
        ImList<Property> result = super.getAndProperties();
        if (implement.property instanceof AndFormulaProperty) {
            AndFormulaProperty andProp = (AndFormulaProperty) implement.property;
            PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>> andImplement
                    = (PropertyImplement<AndFormulaProperty.Interface, PropertyInterfaceImplement<Interface>>) implement;

            PropertyInterfaceImplement<Interface> objectIface = andImplement.mapping.get(andProp.objectInterface);
            if (objectIface instanceof PropertyMapImplement) {
                result = result.addList(((PropertyMapImplement) objectIface).property.getAndProperties()); // сначала inherit'им верхние потом свои
            }
        }
        return result;
    }

    @Override
    public Inferred<Interface> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        if(implement.property instanceof CompareFormulaProperty) {
            CompareFormulaProperty compareProperty = (CompareFormulaProperty) implement.property;
            return compareProperty.inferJoinInterfaceClasses(implement.mapping.getValue(0), implement.mapping.getValue(1), inferType);
        }
        
//        if(implement.property instanceof FormulaUnionProperty) {
//            return op(implement.mapping, implement.property.interfaces.toMap(commonValue), FullFunctionSet.<T>instance(), inferType, true);
//        }
        
        Inferred<T> implementInferred = implement.property.inferInterfaceClasses(commonValue, inferType);
        Inferred<Interface> result = inferJoin(inferType, implementInferred.getParams(inferType), implementInferred.getNotNull());
                                    // пока нет смысла так как таких случаев нет в проекте, а нагрузка увеличивается
//                                    inferJoin(inferType, implementInferred.getParams(inferType), implementInferred.getNotNull()).and(
//                                     inferJoin(inferType, implementInferred.getNotParams(inferType), implementInferred.getNotNotNull()).not(), inferType);

        if(implement.property instanceof NotFormulaProperty)
            result = result.not();
        
        return result;
    }

    private Inferred<Interface> inferJoin(InferType inferType, ImMap<T, ExClassSet> implementParams, ImSet<T>[] implementNotNull) {
        if(implementParams == null)
            return Inferred.FALSE();
        return op(implement.mapping, implementParams, implementNotNull, inferType); // возможно здесь надо было бы отдельно для прямой ветки, а отдельно для not*, но как их слить пока неясно
    }

    @Override
    public boolean needInferredForValueClass(InferType inferType) {
        if(implement.property.needInferredForValueClass(inferType))
            return false;
        // in theory check for explicitClasses could be done, but it's not that important
        return opNeedInferForValueClass(implement.mapping.values(), inferType);
    }

    public ValueClass objectPropertyClass; // временный хак
    @Override
    public ExClassSet calcInferValueClass(final ImMap<Interface, ExClassSet> inferred, final InferType inferType) {
        ExClassSet result = implement.property.inferJoinValueClass(implement.mapping.mapValues(value -> value.mapInferValueClass(inferred, inferType)), !user, inferType);
        if(objectPropertyClass != null)
            result = ExClassSet.op(result, ExClassSet.toExValue(objectPropertyClass), false);
        return result;
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull() && implement.property.isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new JoinDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.join}"), this, LM
        );
    }

    @Override
    public ActionMapImplement<?, Interface> getSetNotNullAction(boolean notNull) {
        ImRevMap<T,Interface> identityMap = PropertyInterface.getIdentityMap(implement.mapping);
        if(identityMap == null)
            return super.getSetNotNullAction(notNull);

        return implement.property.getSetNotNullAction(notNull).map(identityMap);
    }

    @Override
    public boolean isNotNull() {
        if (super.isNotNull()) {
            return true;
        }
        if (implement.mapping.size() == 1 && implement.mapping.singleValue() instanceof PropertyMapImplement) {
            return ((PropertyMapImplement) implement.mapping.singleValue()).property.isNotNull();    
        }
        return false;
    }


    @Override
    public InputListEntity<?, Interface> getFilterInputList(ImMap<Interface, StaticParamNullableExpr> fixedExprs, boolean noJoin) {
        if(!noJoin) {
            ImRevMap<T, Interface> mapInterfaces = BaseUtils.immutableCast(implement.mapping.filterFnValues(element -> element instanceof Interface && fixedExprs.containsKey((Interface) element)).toRevMap());
            ImMap<T, StaticParamNullableExpr> mapFixedExprs = mapInterfaces.join(fixedExprs);
            // using top most property or bottom most property / property itself
            if (implement.property.isValueFull(mapFixedExprs) && (
                    implement.property.isValueUnique(mapFixedExprs, true) ||
                            !isValueFull(fixedExprs) ||
                            isValueUnique(getValueStat(fixedExprs), implement.property.getValueStat(mapFixedExprs))))
                return new InputListEntity<>(implement.property, mapInterfaces);
        }

        return super.getFilterInputList(fixedExprs, noJoin);
    }
}
