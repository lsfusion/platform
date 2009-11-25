package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.session.DataChanges;
import platform.server.session.Modifier;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;

import java.util.*;

public class DefaultData<D extends PropertyInterface> {

    PropertyImplement<PropertyInterfaceImplement<DataPropertyInterface>,D> data;
    Collection<PropertyMapImplement<?,DataPropertyInterface>> onChange = new ArrayList<PropertyMapImplement<?, DataPropertyInterface>>();
    boolean defaultChanged = false;

    protected void fillDepends(Set<Property> depends) {
        if(defaultChanged) depends.add(data.property);
    }
    
    public DefaultData(PropertyImplement<PropertyInterfaceImplement<DataPropertyInterface>,D> data, Collection<PropertyMapImplement<?,DataPropertyInterface>> onChange, boolean defaultChanged) {
        this.data = data;
        this.onChange = onChange;
        this.defaultChanged = defaultChanged;
    }

    public <U extends DataChanges<U>> U getUsedChanges(Modifier<U> modifier) {

        Set<Property> used = new HashSet<Property>();
        FunctionProperty.fillDepends(used,BaseUtils.merge(data.mapping.values(),onChange));
        if(defaultChanged) used.add(data.property);
        return Property.getUsedChanges(used, modifier);
    }

    public Expr getExpr(Map<DataPropertyInterface, ? extends Expr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, Expr> implementExprs = new HashMap<D, Expr>();
        for(Map.Entry<D,PropertyInterfaceImplement<DataPropertyInterface>> interfaceImplement : data.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapExpr(joinImplement, modifier, onChangeWhere));

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,DataPropertyInterface> propChange : onChange)
            andWhere = andWhere.and(propChange.mapExpr(joinImplement, modifier, onChangeWhere).getWhere());

        changedWhere.add(andWhere.and(onChangeWhere.toWhere()));
        if(defaultChanged)
            return data.property.getExpr(implementExprs, modifier, null);
        else
            return data.property.getExpr(implementExprs);
    }
}
