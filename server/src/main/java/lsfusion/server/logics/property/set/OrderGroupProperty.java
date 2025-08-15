package lsfusion.server.logics.property.set;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.ModalityWindowFormType;
import lsfusion.server.base.caches.IdentityStrongLazy;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.WindowExpr;
import lsfusion.server.data.expr.formula.FormulaExpr;
import lsfusion.server.data.expr.formula.FormulaJoinImpl;
import lsfusion.server.data.expr.key.KeyExpr;
import lsfusion.server.data.expr.query.AggrExpr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.language.action.LA;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.flow.ForAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.form.interactive.action.edit.FormSessionScope;
import lsfusion.server.logics.form.stat.SelectTop;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class OrderGroupProperty<I extends PropertyInterface> extends GroupProperty<I> {

    private final ImList<PropertyInterfaceImplement<I>> props;
    public ImList<PropertyInterfaceImplement<I>> getProps() {
        return props;
    }

    private final GroupType groupType;
    public GroupType getGroupType() {
        return groupType;
    }

    private final ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders;
    private final boolean ordersNotNull;
    public ImOrderMap<PropertyInterfaceImplement<I>, Boolean> getOrders() {
        return orders;
    }

    // groups should (and only them) contain window interfaces
    private final SelectTop<I> selectTop;
    private final SelectTop<Interface<I>> mapSelectTop;

    public boolean getOrdersNotNull() {
        return ordersNotNull;
    }

    @Override
    public boolean isNameValueUnique() {
        if(groupType == GroupType.CONCAT) // similar to concat formula
            return true;

        return super.isNameValueUnique();
    }

    public OrderGroupProperty(LocalizedString caption, ImSet<I> innerInterfaces, ImCol<? extends PropertyInterfaceImplement<I>> groupInterfaces, ImList<PropertyInterfaceImplement<I>> props, PropertyInterfaceImplement<I> nameProp, PropertyInterfaceImplement<I> whereProp, GroupType groupType, ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, SelectTop<I> selectTop) {
        super(caption, innerInterfaces, groupInterfaces);
        this.props = props;
        this.groupType = groupType;
        this.orders = orders;
        this.ordersNotNull = ordersNotNull;

        this.nameProp = nameProp;
        this.whereProp = whereProp;
        assert groupType == GroupType.CONCAT || (whereProp == null && nameProp == null);

        this.selectTop = selectTop;
        this.mapSelectTop = selectTop.mapValues(BaseUtils.<ImRevMap<I, Interface<I>>>immutableCast(getMapRevInterfaces()));

        finalizeInit();
    }

    private final PropertyInterfaceImplement<I> nameProp;
    private final PropertyInterfaceImplement<I> whereProp;

    public static <I extends PropertyInterface<I>> OrderGroupProperty<I> create(LocalizedString caption, ImSet<I> innerInterfaces, ImCol<? extends PropertyInterfaceImplement<I>> groupInterfaces, ImList<PropertyInterfaceImplement<I>> props, PropertyInterfaceImplement<I> whereProp, GroupType groupType, ImOrderMap<PropertyInterfaceImplement<I>, Boolean> orders, boolean ordersNotNull, SelectTop<I> selectTop) {

        PropertyInterfaceImplement<I> nameProp = null;
        if(groupType == GroupType.CONCAT) {
            nameProp = props.get(0);
            if(whereProp != null) {
                PropertyMapImplement<?, I> and = PropertyFact.createAnd(innerInterfaces, nameProp, whereProp);
                and.property.caption = caption;
                props = ListFact.toList(and, props.get(1));
            } else { // splitting into if
                Pair<PropertyInterfaceImplement<I>, PropertyInterfaceImplement<I>> ifProp = nameProp.getIfProp();
                if(ifProp != null) {
                    nameProp = ifProp.first;
                    whereProp = ifProp.second;
                }
            }
//            assert groupProps.toSet().containsAll(innerInterfaces.removeIncl(aggrInterface));
        }

        return new OrderGroupProperty<>(caption, innerInterfaces, groupInterfaces, props, nameProp, whereProp, groupType, orders, ordersNotNull, selectTop);
    }

    public static <I extends PropertyInterface> ImRevMap<I, KeyExpr> getMapKeys(ImSet<I> innerInterfaces, SelectTop<I> selectTop) {
        ImRevMap<I, KeyExpr> mapKeys = KeyExpr.getMapKeys(innerInterfaces);

        if(!selectTop.isEmpty())
            mapKeys = mapKeys.mapRevValues((anInterface, expr) -> {
                WindowExpr windowExpr = selectTop.getParamExpr(anInterface);
                return windowExpr != null ? windowExpr : expr;
            });

        return mapKeys;
    }
    @Override
    @IdentityStrongLazy
    public <X extends PropertyInterface, V extends PropertyInterface, W extends PropertyInterface> Select<Interface<I>> getSelectProperty(ImList<Property> viewProperties, boolean forceSelect) {
        ImRevMap<Interface<I>, I> groupMap = getConcatMap();
        if(groupMap != null)
            return Property.getSelectProperty(getBaseLM(), true, false, forceSelect, groupMap, innerInterfaces, (PropertyMapImplement<?, I>) nameProp, whereProp, null, (PropertyMapImplement<?, I>) nameProp, orders);

        return super.getSelectProperty(viewProperties, forceSelect);
    }

    @Override
    @IdentityStrongLazy
    public ActionMapImplement<?, Interface<I>> getDefaultEventAction(String eventActionSID, FormSessionScope defaultChangeEventScope, ImList<Property> viewProperties, String customChangeFunction) {
        ImRevMap<Interface<I>, I> groupMap = getConcatMap();
        if(groupMap != null && !eventActionSID.equals(ServerResponse.EDIT_OBJECT)) {
            BaseLogicsModule baseLM = getBaseLM();

            PropertyMapImplement<ClassPropertyInterface, I> selectProp = ForAction.createForDataProp((PropertyMapImplement<?, I>) whereProp, groupMap, null, null);
            ActionMapImplement<?, Interface<I>> writeAction = PropertyFact.createSetAction(innerInterfaces, groupMap, selectProp, whereProp);

            // now Object.noClasses doesn't work for interactive forms (so we can return innerClasses to null if it will be fixed)
            LogicsModule.IntegrationForm<I> selectForm = Property.getSelectForm(baseLM, innerInterfaces, getInnerInterfaceClasses(), groupMap.valuesSet(), (PropertyMapImplement<?, I>) nameProp, selectProp, (PropertyMapImplement<?, I>) nameProp, orders, false, true);
            LA<?> la = baseLM.addIFAProp(null, LocalizedString.NONAME, selectForm.form, selectForm.objectsToSet, BaseUtils.nvl(defaultChangeEventScope, PropertyDrawEntity.DEFAULT_CUSTOMCHANGE_EVENTSCOPE), true, ModalityWindowFormType.EMBEDDED, false);
            ActionMapImplement<?, Interface<I>> selectFormAction = la.getImplement(selectForm.getOrderInterfaces(groupMap));

            ActionMapImplement<?, Interface<I>> result = PropertyFact.createRequestAction(interfaces,
                    PropertyFact.createListAction(interfaces, writeAction, selectFormAction),
                    PropertyFact.createSetAction(innerInterfaces, groupMap, (PropertyMapImplement<?, I>) whereProp, selectProp), null);
            return result;
        }

        return super.getDefaultEventAction(eventActionSID, defaultChangeEventScope, viewProperties, customChangeFunction);
    }

    private ImRevMap<Interface<I>, I> getConcatMap() {
        if(groupType == GroupType.CONCAT && nameProp instanceof PropertyMapImplement && whereProp instanceof PropertyMapImplement &&
                ((PropertyMapImplement<?, ?>) whereProp).property.canBeChanged(false) &&
                ((PropertyMapImplement<?, ?>) whereProp).property.getType() instanceof LogicalClass && selectTop.isEmpty())
            return PropertyInterface.getIdentityMap(getMapInterfaces());
        return null;
    }

    protected ImMap<I, Expr> getGroupKeys(ImMap<Interface<I>, ? extends Expr> joinImplement) {
        return MapFact.override(getMapKeys(innerInterfaces, selectTop), BaseUtils.immutableCast(getMapRevInterfaces().join((ImMap<Interface<I>, Expr>)joinImplement).filterFn((key, value) -> value.isValue() && key instanceof PropertyInterface && !selectTop.contains((I) key))));
    }

    protected Expr calculateExpr(ImMap<Interface<I>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        // если нужна инкрементность
        ImMap<I, Expr> mapKeys = getGroupKeys(joinImplement);

        if(checkPrereadNull(mapKeys, calcType, propChanges, changedWhere != null))
            return Expr.NULL();

        WhereBuilder changedGroupWhere = cascadeWhere(changedWhere);

        ImList<Expr> exprs = getExprImplements(mapKeys, calcType, propChanges, changedGroupWhere);
        ImMap<Interface<I>, Expr> groups = getGroupImplements(mapKeys, calcType, propChanges, changedGroupWhere);
        ImOrderMap<Expr, Boolean> orders = getOrderImplements(mapKeys, calcType, propChanges, changedGroupWhere);

        // we do this to make map in all group maps reversable
        if(!selectTop.isEmpty()) {
            joinImplement = joinImplement.mapValues((anInterface, expr) -> {
                FormulaJoinImpl formula = mapSelectTop.getParamFormula(anInterface);
                return (formula != null ? FormulaExpr.create(formula, ListFact.singleton(expr)) : expr);
            });
        }

        GroupType groupType = getGroupType();
        if(changedWhere!=null) {
            assert calcType.isExpr();
            changedWhere.add(getPartitionWhere(changedGroupWhere.toWhere(), groupType, groups, exprs, orders, joinImplement));
            PropertyChanges prevChanges = getPrevPropChanges(calcType, propChanges);
            changedWhere.add(getPartitionWhere(changedGroupWhere.toWhere(), groupType, getGroupImplements(mapKeys, prevChanges),
                    getExprImplements(mapKeys, prevChanges), getOrderImplements(mapKeys, prevChanges), joinImplement));
        }

        return GroupExpr.create(groups, exprs, orders, ordersNotNull, groupType, joinImplement, calcType instanceof CalcClassType);
    }

    protected boolean useSimpleIncrement() {
        return true;
    }

    protected Where getPartitionWhere(Where where, GroupType groupType, ImMap<Interface<I>, Expr> groups, ImList<Expr> exprs, ImOrderMap<Expr, Boolean> orders, ImMap<Interface<I>, ? extends Expr> joinImplement) {
        if(!selectTop.isEmpty()) {
            groups = groups.remove(mapSelectTop.getParamsSet());
            joinImplement = joinImplement.remove(mapSelectTop.getParamsSet());
        }
        return GroupExpr.create(groups, where.and(groupType.getWhere(exprs).and(AggrExpr.getOrderWhere(orders, ordersNotNull))), joinImplement).getWhere();
    }
}
