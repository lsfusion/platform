package lsfusion.server.logics.property.set;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MOrderExclSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.ComplexIncrementProperty;
import lsfusion.server.logics.property.JoinProperty;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.GroupDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.List;
import java.util.function.Function;

abstract public class GroupProperty<I extends PropertyInterface> extends ComplexIncrementProperty<GroupProperty.Interface<I>> {

    public static class Interface<I extends PropertyInterface> extends PropertyInterface<Interface<I>> {
        public PropertyInterfaceImplement<I> implement;

        public Interface(int ID, PropertyInterfaceImplement<I> implement) {
            super(ID);
            this.implement = implement;
        }
    }

    protected final ImSet<I> innerInterfaces;

    private GroupProperty(LocalizedString caption, ImOrderSet<Interface<I>> interfaces, ImSet<I> innerInterfaces) {
        super(caption, interfaces);
        this.innerInterfaces = innerInterfaces;
    }

    protected GroupProperty(LocalizedString caption, ImSet<I> innerInterfaces, ImCol<? extends PropertyInterfaceImplement<I>> groupInterfaces) {
        this(caption, getInterfaces(groupInterfaces), innerInterfaces);
    }

    protected GroupProperty(LocalizedString caption, ImSet<I> innerInterfaces, ImList<? extends PropertyInterfaceImplement<I>> groupInterfaces) {
        this(caption, getTempInterfaces(groupInterfaces), innerInterfaces);
    }
    
    public ImMap<Interface<I>, PropertyInterfaceImplement<I>> getMapInterfaces() {
        return interfaces.mapValues((Function<Interface<I>, PropertyInterfaceImplement<I>>) value -> value.implement);
    }

    public abstract GroupType getGroupType();

    public abstract ImList<PropertyInterfaceImplement<I>> getProps();

    public abstract ImOrderMap<PropertyInterfaceImplement<I>, Boolean> getOrders();
    public abstract boolean getOrdersNotNull();

    public Inferred<I> inferInnerInterfaceClasses(final ExClassSet commonValue, InferType inferType) {
        GroupType groupType = getGroupType();
        return inferInnerInterfaceClasses(getProps().addList(getMapInterfaces().values().toList()),
                groupType.isSelect(), commonValue, getOrders(), getOrdersNotNull(), groupType.getSkipWhereIndex(), inferType);
    }
    public Inferred<I> inferInnerInterfaceClasses(final ImMap<Interface<I>, ExClassSet> inferred, InferType inferType) {
        ImList<PropertyInterfaceImplement<I>> props = getProps();
        ImOrderSet<Interface<I>> orderInterfaces = getOrderInterfaces();
        return inferInnerInterfaceClasses(props.addList(orderInterfaces.mapList(getMapInterfaces())), getOrders(), getOrdersNotNull(), getGroupType().getSkipWhereIndex(), ListFact.toList((ExClassSet) null, props.size()).addList(orderInterfaces.mapList(inferred)), inferType);
    }
    public ExClassSet inferInnerValueClass(final ImMap<I, ExClassSet> commonClasses, InferType inferType) {
        return inferInnerValueClass(getProps(), commonClasses, getGroupType(), inferType);
    }

    @Override
    public Inferred<Interface<I>> calcInferInterfaceClasses(final ExClassSet commonValue, final InferType inferType) {
        final ImMap<I, ExClassSet> innerInferred = inferInnerInterfaceClasses(commonValue, inferType).finishEx(inferType);
        if(innerInferred == null)
            return Inferred.FALSE();

        ImMap<Interface<I>, PropertyInterfaceImplement<I>> mapInterfaces = getMapInterfaces();
        return new Inferred<>(mapInterfaces.mapValues(value -> ExClassSet.toNotNull(value.mapInferValueClass(innerInferred, inferType))));
    }

    protected ImMap<I, ResolveClassSet> explicitInnerClasses; // без nulls
    
    public void setExplicitInnerClasses(ImOrderSet<I> innerInterfaces, List<ResolveClassSet> explicitInnerClasses) {
        this.explicitInnerClasses = getPackedSignature(innerInterfaces, explicitInnerClasses);
        assert this.explicitInnerClasses != null;
    }

    public ExClassSet calcInferValueClass(final ImMap<Interface<I>, ExClassSet> inferred, final InferType inferType) {
        ImMap<I, ExClassSet> innerInferred;
        if(inferType != InferType.resolve()) {
            innerInferred = getInferExplicitCalcInterfaces(innerInterfaces, noOld(), inferType, explicitInnerClasses, () -> inferInnerInterfaceClasses(inferred, inferType).finishEx(inferType), "CALCINNER", this, null);
        } else {
            assert explicitInnerClasses != null;
            innerInferred = ExClassSet.toEx(explicitInnerClasses);
        }
        if (innerInferred == null)
            return ExClassSet.FALSE;
        return inferInnerValueClass(innerInferred, inferType);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(ImMap<I, ? extends Expr> mapKeys, PropertyChanges changes) {
        return getGroupImplements(mapKeys, CalcType.EXPR, changes);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(ImMap<I, ? extends Expr> mapKeys, CalcType calcType, PropertyChanges changes) {
        return getGroupImplements(mapKeys, calcType, changes, null);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(ImMap<I, ? extends Expr> mapKeys, PropertyChanges changes, WhereBuilder changedWhere) {
        return getGroupImplements(mapKeys, CalcType.EXPR, changes, changedWhere);
    }

    protected ImMap<Interface<I>, Expr> getGroupImplements(final ImMap<I, ? extends Expr> mapKeys, final CalcType calcType, final PropertyChanges changes, final WhereBuilder changedWhere) {
        return interfaces.mapItValues(value -> value.implement.mapExpr(mapKeys, calcType, changes, changedWhere));
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(ImMap<I, ? extends Expr> joinImplement, PropertyChanges changes) {
        return getOrderImplements(joinImplement, CalcType.EXPR, changes);
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(ImMap<I, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges changes) {
        return getOrderImplements(joinImplement, calcType, changes, null);
    }

    protected ImOrderMap<Expr, Boolean> getOrderImplements(final ImMap<I, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges changes, final WhereBuilder changedWhere) {
        return getOrders().mapMergeItOrderKeys(value -> value.mapExpr(joinImplement, calcType, changes, changedWhere));
    }

    protected ImList<Expr> getExprImplements(ImMap<I, ? extends Expr> joinImplement, PropertyChanges changes) {
        return getExprImplements(joinImplement, CalcType.EXPR, changes, null);
    }

    protected ImList<Expr> getExprImplements(ImMap<I, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges changes) {
        return getExprImplements(joinImplement, calcType, changes, null);
    }

    protected ImList<Expr> getExprImplements(final ImMap<I, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges changes, final WhereBuilder changedWhere) {
        return getProps().mapItListValues(value -> value.mapExpr(joinImplement, calcType, changes, changedWhere));
    }

    // не очень хорошо, так как берет на себя часть функций компилятора (проталкивание значений), но достаточно неплохо должна помогать оптимизации
    protected ImMap<I, Expr> getGroupKeys(ImMap<Interface<I>, ? extends Expr> joinImplement) {
        ImMap<I, ? extends Expr> interfaceValues = BaseUtils.immutableCast(getMapInterfaces().toRevExclMap().reverse().join((ImMap<Interface<I>, Expr>)joinImplement).filterFn((key, value) -> value.isValue() && key instanceof PropertyInterface));
        return MapFact.override(KeyExpr.getMapKeys(innerInterfaces), interfaceValues);
    }

    @Override
    public void fillDepends(MSet<Property> depends, boolean events) {
        for(Interface interfaceImplement : interfaces)
            interfaceImplement.implement.mapFillDepends(depends);
        fillDepends(depends, getProps().getCol());
        fillDepends(depends, getOrders().keys());
    }

    public ImMap<I, ValueClass> getInnerInterfaceClasses() {
        InferType inferType = InferType.prevSame();
        return ExClassSet.fromExValue(inferInnerInterfaceClasses((ExClassSet) null, inferType).finishEx(inferType));
/*        ImRevMap<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);
        Where w = Expr.getWhere(getGroupImplements(mapKeys, PropertyChanges.EMPTY))
                .and(Expr.getWhere(getExprImplements(mapKeys, PropertyChanges.EMPTY)))
                .and(Expr.getWhere(getOrderImplements(mapKeys, PropertyChanges.EMPTY).keys()))
                ;

        ClassWhere<I> classWhere = w.getClassWhere().get(mapKeys);

        return classWhere.getCommonParent(innerInterfaces);*/
    }

    protected boolean checkPrereadNull(ImMap<I, ? extends Expr> joinImplement, final CalcType calcType, final PropertyChanges propChanges) {
        return JoinProperty.checkPrereadNull(joinImplement, true, getProps().getCol(), calcType, propChanges) ||
                JoinProperty.checkPrereadNull(joinImplement, true, interfaces.mapSetValues(value -> value.implement), calcType, propChanges) ||
                JoinProperty.checkPrereadNull(joinImplement, getOrdersNotNull(), getOrders().keys(), calcType, propChanges);
    }

    @Override
    public boolean supportsDrillDown() {
        return isDrillFull();
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM, String canonicalName) {
        return new GroupDrillDownFormEntity(
                canonicalName, LocalizedString.create("{logics.property.drilldown.form.group}." + getGroupType().name().toLowerCase()), this, LM
        );
    }

    private static <I extends PropertyInterface> ImOrderSet<Interface<I>> getTempInterfaces(ImList<? extends PropertyInterfaceImplement<I>> interfaceImplements) {
        MOrderExclSet<Interface<I>> mResult = SetFact.mOrderExclSet(interfaceImplements.size());
        for (int i = 0, size = interfaceImplements.size(); i < size; i++)
            mResult.exclAdd(new Interface<>(i, interfaceImplements.get(i)));
        return mResult.immutableOrder();
    }
    
    private static <I extends PropertyInterface> ImOrderSet<Interface<I>> getInterfaces(ImCol<? extends PropertyInterfaceImplement<I>> interfaceImplements) {
        return interfaceImplements.mapColSetValues(
                Interface::new).toOrderSet();
    }
}
