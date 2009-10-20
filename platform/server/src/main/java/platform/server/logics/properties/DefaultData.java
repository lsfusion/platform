package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.DataChanges;
import platform.server.session.Modifier;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

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

    public SourceExpr getSourceExpr(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, TableModifier<? extends TableChanges> modifier, WhereBuilder changedWhere) {

        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, SourceExpr> implementExprs = new HashMap<D, SourceExpr>();
        for(Map.Entry<D,PropertyInterfaceImplement<DataPropertyInterface>> interfaceImplement : data.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapSourceExpr(joinImplement, modifier, onChangeWhere));

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,DataPropertyInterface> propChange : onChange)
            andWhere = andWhere.and(propChange.mapSourceExpr(joinImplement, modifier, onChangeWhere).getWhere());

        changedWhere.add(andWhere.and(onChangeWhere.toWhere()));
        if(defaultChanged)
            return data.property.getSourceExpr(implementExprs, modifier, null);
        else
            return data.property.getSourceExpr(implementExprs);
    }
}
