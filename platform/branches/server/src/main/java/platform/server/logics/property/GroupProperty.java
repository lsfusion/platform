package platform.server.logics.property;

import platform.server.Settings;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.*;

abstract public class GroupProperty<T extends PropertyInterface> extends FunctionProperty<GroupProperty.Interface<T>> {

    protected final boolean SIMPLE_SCHEME = true;

    // оператор
    int operator;

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

    protected GroupProperty(String sID, String caption, Collection<? extends PropertyInterfaceImplement<T>> interfaces, Property<T> property, int operator) {
        super(sID, caption, getInterfaces(interfaces));
        groupProperty = property;
        this.operator = operator;
    }

    // группировочное св-во
    protected Property<T> groupProperty;

    Object groupValue = "grfield";

    protected Map<Interface<T>, Expr> getGroupImplements(Map<T, KeyExpr> mapKeys, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        Map<Interface<T>, Expr> group = new HashMap<Interface<T>, Expr>();
        for(Interface<T> propertyInterface : interfaces)
            group.put(propertyInterface,propertyInterface.implement.mapExpr(mapKeys, modifier, changedWhere));
        return group;
    }

    public Expr calculateExpr(Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {

        if(!hasChanges(modifier) || (changedWhere==null && (!isStored() || (Settings.NO_INCREMENT_MAX_GROUP_PROPERTY && this instanceof MaxGroupProperty)))) return calculateNewExpr(joinImplement, modifier);

        // если нужна инкрементность
        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys(); // изначально чтобы новые и старые группировочные записи в одном контексте были
        
        // новые группировочные записи
        WhereBuilder changedGroupWhere = new WhereBuilder();
        Expr changedExpr = GroupExpr.create(getGroupImplements(mapKeys, modifier, changedGroupWhere), groupProperty.getExpr(mapKeys, modifier, changedGroupWhere), changedGroupWhere.toWhere(), operator != 1, joinImplement);

        // старые группировочные записи
        Expr changedPrevExpr = GroupExpr.create(getGroupImplements(mapKeys, defaultModifier, null), groupProperty.getExpr(mapKeys), changedGroupWhere.toWhere(), operator != 1, joinImplement);

        if(changedWhere!=null) changedWhere.add(changedExpr.getWhere().or(changedPrevExpr.getWhere())); // если хоть один не null
        return getChangedExpr(changedExpr, changedPrevExpr, joinImplement, modifier);
    }

    protected Expr calculateNewExpr(Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier) {
        Map<T, KeyExpr> mapKeys = groupProperty.getMapKeys();
        return GroupExpr.create(getGroupImplements(mapKeys, modifier, null), groupProperty.getExpr(mapKeys, modifier, null), operator != 1, joinImplement);
    }

    abstract Expr getChangedExpr(Expr changedExpr, Expr changedPrevExpr, Map<Interface<T>, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier);

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
