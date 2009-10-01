package platform.server.logics.properties;

import platform.base.BaseUtils;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.DataChanges;
import platform.server.session.TableChanges;
import platform.server.where.Where;
import platform.server.where.WhereBuilder;

import java.util.*;

public class DefaultData<D extends PropertyInterface> {

    PropertyImplement<PropertyInterfaceImplement<DataPropertyInterface>,D> data;
    Collection<PropertyMapImplement<?,DataPropertyInterface>> onChange = new ArrayList<PropertyMapImplement<?, DataPropertyInterface>>();
    
    public DefaultData(PropertyImplement<PropertyInterfaceImplement<DataPropertyInterface>,D> data, Collection<PropertyMapImplement<?,DataPropertyInterface>> onChange) {
        this.data = data;
        this.onChange = onChange;
    }

    public <C extends DataChanges<C>,U extends Property.UsedChanges<C,U>> U getUsedChanges(C changes, Collection<DataProperty> usedDefault, Property.Depends<C,U> depends) {

        Set<Property> used = new HashSet<Property>();
        FunctionProperty.fillDepends(used,BaseUtils.merge(data.mapping.values(),onChange));
        return Property.getUsedChanges(used, changes, usedDefault, depends);
    }

    public SourceExpr getSourceExpr(Map<DataPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, Property.TableDepends<? extends Property.TableUsedChanges> depends, WhereBuilder changedWhere) {

        WhereBuilder onChangeWhere = new WhereBuilder();
        Map<D, SourceExpr> implementExprs = new HashMap<D, SourceExpr>();
        for(Map.Entry<D,PropertyInterfaceImplement<DataPropertyInterface>> interfaceImplement : data.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapSourceExpr(joinImplement, session, usedDefault, depends, onChangeWhere));

        Where andWhere = Where.TRUE; // докинем дополнительные условия
        for(PropertyMapImplement<?,DataPropertyInterface> propChange : onChange)
            andWhere = andWhere.and(propChange.mapSourceExpr(joinImplement, session, usedDefault, depends, onChangeWhere).getWhere());

        changedWhere.add(andWhere.and(onChangeWhere.toWhere()));        
        return data.property.getSourceExpr(implementExprs);
    }
}
