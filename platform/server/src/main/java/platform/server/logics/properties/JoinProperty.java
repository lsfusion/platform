package platform.server.logics.properties;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;
import platform.base.BaseUtils;

import java.util.*;

public class JoinProperty<T extends PropertyInterface> extends FunctionProperty<JoinPropertyInterface> {
    public PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T> implement;

    static Collection<JoinPropertyInterface> getInterfaces(int intNum) {
        Collection<JoinPropertyInterface> interfaces = new ArrayList<JoinPropertyInterface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new JoinPropertyInterface(i));
        return interfaces;
    }

    public JoinProperty(String iSID, int intNum, Property<T> iProperty) {
        super(iSID, getInterfaces(intNum));
    }

    public SourceExpr calculateSourceExpr(Map<JoinPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Collection<DataProperty> usedDefault, TableDepends<? extends TableUsedChanges> depends, WhereBuilder changedWhere) {

        // считаем новые SourceExpr'ы и классы
        Map<T, SourceExpr> implementExprs = new HashMap<T, SourceExpr>();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> interfaceImplement : implement.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapSourceExpr(joinImplement, session, usedDefault, depends, changedWhere));
        return implement.property.getSourceExpr(implementExprs, session, usedDefault, depends, changedWhere);
    }

    protected void fillDepends(Set<Property> depends) {
        fillDepends(depends,implement.mapping.values());
        depends.add(implement.property);       
    }

    @Override
    public MapChangeDataProperty<JoinPropertyInterface> getChangeProperty(Map<JoinPropertyInterface, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        if(implement.mapping.size()!=1) return null;
        return BaseUtils.singleValue(implement.mapping).mapGetChangeProperty(interfaceClasses, securityPolicy, externalID);
    }
}
