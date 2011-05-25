package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.*;

abstract public class GroupProperty<T extends PropertyInterface> extends FunctionProperty<GroupProperty.Interface<T>> {

    protected final boolean SIMPLE_SCHEME = true;

    // оператор
    int groupType;

    public static class Interface<T extends PropertyInterface> extends PropertyInterface<Interface<T>> {
        public PropertyInterfaceImplement<T> implement;

        public Interface(int ID,PropertyInterfaceImplement<T> implement) {
            super(ID);
            this.implement = implement;
        }
    }

    private static <T extends PropertyInterface> List<Interface<T>> getInterfaces(Collection<? extends PropertyInterfaceImplement<T>> interfaceImplements) {
        List<Interface<T>> interfaces = new ArrayList<Interface<T>>();
        for(PropertyInterfaceImplement<T> implement : interfaceImplements)
            interfaces.add(new Interface<T>(interfaces.size(),implement));
        return interfaces;
    }

    protected abstract GroupType getGroupType();

    protected GroupProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<T>> interfaces, Property<T> property) {
        super(sID, caption, getInterfaces(interfaces));
        groupProperty = property;
        this.groupType = groupType;
    }

    // группировочное св-во
    protected Property<T> groupProperty;

    Object groupValue = "grfield";

    protected Map<Interface<T>, Expr> getGroupImplements(Map<T, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) {
        Map<Interface<T>, Expr> group = new HashMap<Interface<T>, Expr>();
        for(Interface<T> propertyInterface : interfaces)
            group.put(propertyInterface,propertyInterface.implement.mapExpr(mapKeys, modifier));
        return group;
    }

    protected Map<Interface<T>, Expr> getGroupImplements(Map<T, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<Interface<T>, Expr> group = new HashMap<Interface<T>, Expr>();
        for(Interface<T> propertyInterface : interfaces)
            group.put(propertyInterface,propertyInterface.implement.mapExpr(mapKeys, modifier, changedWhere));
        return group;
    }

    // не очень хорошо, так как берет на себя часть функций компилятора (проталкивание значений), но достаточно неплохо должна помогать оптимизации
    private Map<T, Expr> getGroupKeys(Map<Interface<T>, ? extends Expr> joinImplement) {
        Map<PropertyInterfaceImplement<T>, Expr> interfaceValues = new HashMap<PropertyInterfaceImplement<T>, Expr>();
        for(Map.Entry<Interface<T>, ? extends Expr> entry : joinImplement.entrySet())
            if(entry.getValue().isValue())
                interfaceValues.put(entry.getKey().implement, entry.getValue());
        return BaseUtils.replace(groupProperty.getMapKeys(), interfaceValues);
    }

    protected boolean noIncrement() {
        return !isStored();
    }

    public Expr calculateExpr(Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        if(!hasChanges(modifier) || (changedWhere==null && noIncrement())) return calculateNewExpr(joinImplement, modifier);

        // если нужна инкрементность
        Map<T, Expr> mapKeys = getGroupKeys(joinImplement); // изначально чтобы новые и старые группировочные записи в одном контексте были
        
        // новые группировочные записи
        WhereBuilder changedGroupWhere = new WhereBuilder();
        Expr changedExpr = GroupExpr.create(getGroupImplements(mapKeys, modifier, changedGroupWhere), groupProperty.getExpr(mapKeys, modifier, changedGroupWhere), changedGroupWhere.toWhere(), getGroupType(), joinImplement);

        // старые группировочные записи
        Expr changedPrevExpr = GroupExpr.create(getGroupImplements(mapKeys, defaultModifier), groupProperty.getExpr(mapKeys), changedGroupWhere.toWhere(), getGroupType(), joinImplement);

        return getChangedExpr(changedExpr, changedPrevExpr, getExpr(joinImplement), joinImplement, modifier, changedWhere);
    }

    protected Expr calculateNewExpr(Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        Map<T, Expr> mapKeys = getGroupKeys(joinImplement);
        return GroupExpr.create(getGroupImplements(mapKeys, modifier), groupProperty.getExpr(mapKeys, modifier), getGroupType(), joinImplement);
    }

    protected abstract Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Expr prevExpr, Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere);

    @Override
    public void fillDepends(Set<Property> depends, boolean derived) {
        for(Interface<T> interfaceImplement : interfaces)
            interfaceImplement.implement.mapFillDepends(depends);
        depends.add(groupProperty);
    }

    @Override
    protected boolean usePreviousStored() {
        return false;
    }

    public Map<Interface<T>,PropertyInterfaceImplement<T>> getMapInterfaces() {
        Map<Interface<T>,PropertyInterfaceImplement<T>> mapInterfaces = new HashMap<Interface<T>, PropertyInterfaceImplement<T>>();
        for(GroupProperty.Interface<T> propertyInterface : interfaces)
            mapInterfaces.put(propertyInterface,propertyInterface.implement);
        return mapInterfaces;
    }
}
