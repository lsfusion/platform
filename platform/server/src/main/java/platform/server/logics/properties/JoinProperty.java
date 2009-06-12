package platform.server.logics.properties;

import platform.server.auth.ChangePropertySecurityPolicy;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.session.DataChanges;
import platform.server.session.MapChangeDataProperty;
import platform.server.session.TableChanges;
import platform.server.where.WhereBuilder;

import java.util.*;

public class JoinProperty<T extends PropertyInterface> extends AggregateProperty<JoinPropertyInterface> {
    public PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T> implement;

    static Collection<JoinPropertyInterface> getInterfaces(int intNum) {
        Collection<JoinPropertyInterface> interfaces = new ArrayList<JoinPropertyInterface>();
        for(int i=0;i<intNum;i++)
            interfaces.add(new JoinPropertyInterface(i));
        return interfaces;
    }

    public JoinProperty(String iSID, int intNum, Property<T> iProperty) {
        super(iSID, getInterfaces(intNum));
        implement = new PropertyImplement<PropertyInterfaceImplement<JoinPropertyInterface>,T>(iProperty);
    }

    public SourceExpr calculateSourceExpr(Map<JoinPropertyInterface, ? extends SourceExpr> joinImplement, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps, WhereBuilder changedWhere) {

        // считаем новые SourceExpr'ы и классы
        Map<T, SourceExpr> implementExprs = new HashMap<T, SourceExpr>();
        for(Map.Entry<T,PropertyInterfaceImplement<JoinPropertyInterface>> interfaceImplement : implement.mapping.entrySet())
            implementExprs.put(interfaceImplement.getKey(),interfaceImplement.getValue().mapSourceExpr(joinImplement, session, defaultProps, changedWhere, noUpdateProps));
        return implement.property.getSourceExpr(implementExprs, session, defaultProps, noUpdateProps, changedWhere);
    }

    protected boolean fillDependChanges(List<Property> changedProperties, DataChanges changes, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) {
        boolean changed = false;
        for(PropertyInterfaceImplement<JoinPropertyInterface> interfaceImplement : implement.mapping.values())
            changed = interfaceImplement.mapFillChanges(changedProperties, changes, noUpdateProps, defaultProps) || changed;
        return implement.property.fillChanges(changedProperties, changes, defaultProps, noUpdateProps) || changed;
    }


    @Override
    public MapChangeDataProperty<JoinPropertyInterface> getChangeProperty(Map<JoinPropertyInterface, ConcreteClass> interfaceClasses, ChangePropertySecurityPolicy securityPolicy, boolean externalID) {
        if(implement.mapping.size()!=1) return null;
        return implement.mapping.values().iterator().next().mapGetChangeProperty(interfaceClasses, securityPolicy, externalID);
    }
}
