package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.QuickSet;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.innerjoins.KeyEqual;
import platform.server.data.query.innerjoins.KeyEquals;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.*;

import java.util.*;

import static platform.base.BaseUtils.mergeSet;

public class DerivedChange<D extends PropertyInterface, C extends PropertyInterface> {

    private final Property<C> writeTo; // что меняем
    private final PropertyImplement<D, PropertyInterfaceImplement<C>> writeFrom;

    private final Collection<PropertyMapImplement<?,C>> where;
    private final Collection<PropertyMapImplement<?,C>> onChange;
    private final IncrementType incrementType;

    public DerivedChange(Property<C> writeTo, PropertyImplement<D, PropertyInterfaceImplement<C>> writeFrom, Collection<PropertyMapImplement<?, C>> where, Collection<PropertyMapImplement<?, C>> onChange, IncrementType incrementType) {
        this.writeTo = writeTo;
        this.writeFrom = writeFrom;
        this.where = where;
        this.onChange = onChange;
        this.incrementType = incrementType;
    }

    public Set<Property> getOnChangeDepends() {
        Set<Property> used = new HashSet<Property>();
        FunctionProperty.fillDepends(used, BaseUtils.merge(writeFrom.mapping.values(), onChange));
        return used;
    }

    public Set<OldProperty> getOnChangeOldDepends() {
        Set<OldProperty> used = new HashSet<OldProperty>();
        for(Property property : getOnChangeDepends()) {
            assert property.getOldDepends().isEmpty();
            used.add(property.getOld());
        }
        return used;
    }

    public Set<Property> getWhereDepends() {
        Set<Property> used = new HashSet<Property>();
        FunctionProperty.fillDepends(used, where);
        return used;
    }

    public Set<Property> getFullWhereDepends() {
        return mergeSet(mergeSet(getOnChangeDepends(), getOnChangeOldDepends()), getWhereDepends());
    }

    public Set<Property> getDepends() {
        Set<Property> used = new HashSet<Property>();
        used.add(writeFrom.property);
        used.addAll(getFullWhereDepends());
        return used;
    }

    public Set<OldProperty> getOldDepends() {
        Set<OldProperty> result = new HashSet<OldProperty>();
        result.addAll(writeFrom.property.getOldDepends());
        for(Property<?> property : getWhereDepends())
            result.addAll(property.getOldDepends());
        for(OldProperty property : getOnChangeOldDepends())
            result.add(property);
        return result;
    }

    private PropertyChange<C> getDerivedChange(PropertyChanges changes) {

        Map<C,KeyExpr> mapKeys = writeTo.getMapKeys();

        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, Expr> implementExprs = new HashMap<D, Expr>();
        for(Map.Entry<D,PropertyInterfaceImplement<C>> interfaceImplement : writeFrom.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapIncrementExpr(mapKeys, changes, onChangeWhere, IncrementType.CHANGE));

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,C> propChange : where) // наплевать на изменения
            andWhere = andWhere.and(propChange.mapExpr(mapKeys, changes).getWhere());
        for(PropertyMapImplement<?,C> propChange : onChange)
            propChange.mapIncrementExpr(mapKeys, changes, onChangeWhere, incrementType);
        andWhere = andWhere.and(onChangeWhere.toWhere());

        return new PropertyChange<C>(mapKeys, getDerivedChangeExpr(implementExprs, changes, andWhere), andWhere);
    }
    
    private Expr getDerivedChangeExpr(Map<D, Expr> implementExprs, PropertyChanges changes, Where andWhere) {
        KeyEquals keyEquals = andWhere.getKeyEquals(); // оптимизация
        if(keyEquals.size==0)
            return Expr.NULL;
        KeyEqual keyEqual;
        if(keyEquals.size == 1 && !(keyEqual=keyEquals.getKey(0)).isEmpty())
            implementExprs = keyEqual.getTranslator().translate(implementExprs);
        return writeFrom.property.getExpr(implementExprs, changes);
    }

    public boolean hasEventChanges(PropertyChanges propChanges) {
        return hasEventChanges(propChanges.getStruct());
    }

    public boolean hasEventChanges(StructChanges changes) {
        return changes.hasChanges(changes.getUsedChanges(getDepends())); // если в where нет изменений то из assertion'а что
    }

    public QuickSet<Property> getUsedDataChanges(StructChanges changes) {
        assert hasEventChanges(changes);
        return QuickSet.add(writeTo.getUsedDataChanges(changes), changes.getUsedChanges(getDepends()));
    }
    
    public DataChanges getDataChanges(PropertyChanges changes) {
        return writeTo.getDataChanges(getDerivedChange(changes), changes, null).changes;
    }
}
