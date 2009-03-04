package platform.server.logics.properties;

import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.ObjectValue;
import platform.server.logics.auth.ChangePropertySecurityPolicy;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ValueClassSet;
import platform.server.logics.session.ChangeValue;
import platform.server.logics.session.DataChanges;
import platform.server.logics.session.DataSession;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertyMapImplement<T extends PropertyInterface,P extends PropertyInterface> extends PropertyImplement<P,T> implements PropertyInterfaceImplement<P> {

    public PropertyMapImplement(Property<T> iProperty) {super(iProperty);}

    // NotNull только если сессии нету
    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> joinImplement, InterfaceClassSet<P> joinClasses) {
        return property.getSourceExpr(getMapImplement(joinImplement), joinClasses.mapBack(mapping));
    }

    public JoinExpr mapChangeExpr(DataSession session, Map<P, ? extends SourceExpr> joinImplement, int value) {
        return session.getChange(property).getExpr(getMapImplement(joinImplement), value);
    }

    private <V> Map<T, V> getMapImplement(Map<P, V> joinImplement) {
        Map<T,V> mapImplement = new HashMap<T,V>();
        for(T implementInterface : property.interfaces)
            mapImplement.put(implementInterface, joinImplement.get(mapping.get(implementInterface)));
        return mapImplement;
    }

    public ClassSet mapValueClass(InterfaceClass<P> classImplement) {
        return property.getValueClass(classImplement.mapBack(mapping));
    }

    public boolean mapIsInInterface(InterfaceClassSet<P> classImplement) {
        return property.isInInterface(classImplement.mapBack(mapping));
    }

    public InterfaceClassSet<P> mapClassSet(ClassSet reqValue) {
        return property.getClassSet(reqValue).map(mapping);
    }

    public ValueClassSet<P> mapValueClassSet() {
        return property.getValueClassSet().map(mapping);
    }

    public boolean mapHasChanges(DataSession session) {
        return session.getChange(property)!=null;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate) {
        return property.fillChangedList(changedProperties, changes, noUpdate);
    }

    ChangeValue mapGetChangeProperty(DataSession session, Map<P, ObjectValue> keys, int coeff, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        return property.getChangeProperty(session,getMapImplement(keys), coeff, securityPolicy);
    }

    // для OverrideList'а по сути
    void mapChangeProperty(Map<P, ObjectValue> keys, Object newValue, boolean externalID, DataSession session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        property.changeProperty(getMapImplement(keys), newValue, externalID, session, securityPolicy);
    }

    public ClassSet mapChangeValueClass(DataSession session, InterfaceClass<P> classImplement) {
        return session.getChange(property).classes.getValueClass(classImplement.mapBack(mapping));
    }

    public InterfaceClassSet<P> mapChangeClassSet(DataSession session, ClassSet reqValue) {
        return session.getChange(property).classes.getClassSet(reqValue).map(mapping);
    }

    public ValueClassSet<P> mapValueClassSet(DataSession session) {
        return session.getChange(property).classes.getValueClassSet().map(mapping);
    }
}
