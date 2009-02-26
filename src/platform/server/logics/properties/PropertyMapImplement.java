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
    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses) {
        return property.getSourceExpr(getMapImplement(JoinImplement),JoinClasses.mapBack(mapping));
    }

    public JoinExpr mapChangeExpr(DataSession Session, Map<P, ? extends SourceExpr> JoinImplement, int Value) {
        return Session.getChange(property).getExpr(getMapImplement(JoinImplement),Value);
    }

    private <V> Map<T, V> getMapImplement(Map<P, V> JoinImplement) {
        Map<T,V> MapImplement = new HashMap<T,V>();
        for(T ImplementInterface : property.interfaces)
            MapImplement.put(ImplementInterface,JoinImplement.get(mapping.get(ImplementInterface)));
        return MapImplement;
    }

    public ClassSet mapValueClass(InterfaceClass<P> ClassImplement) {
        return property.getValueClass(ClassImplement.mapBack(mapping));
    }

    public boolean mapIsInInterface(InterfaceClassSet<P> ClassImplement) {
        return property.isInInterface(ClassImplement.mapBack(mapping));
    }

    public InterfaceClassSet<P> mapClassSet(ClassSet ReqValue) {
        return property.getClassSet(ReqValue).map(mapping);
    }

    public ValueClassSet<P> mapValueClassSet() {
        return property.getValueClassSet().map(mapping);
    }

    public boolean mapHasChanges(DataSession Session) {
        return Session.getChange(property)!=null;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        return property.fillChangedList(ChangedProperties, Changes, NoUpdate);
    }

    ChangeValue mapGetChangeProperty(DataSession Session, Map<P, ObjectValue> Keys, int Coeff, ChangePropertySecurityPolicy securityPolicy) {
        return property.getChangeProperty(Session,getMapImplement(Keys), Coeff, securityPolicy);
    }

    // для OverrideList'а по сути
    void mapChangeProperty(Map<P, ObjectValue> Keys, Object NewValue, boolean externalID, DataSession Session, ChangePropertySecurityPolicy securityPolicy) throws SQLException {
        property.changeProperty(getMapImplement(Keys), NewValue, externalID, Session, securityPolicy);
    }

    public ClassSet mapChangeValueClass(DataSession Session, InterfaceClass<P> ClassImplement) {
        return Session.getChange(property).classes.getValueClass(ClassImplement.mapBack(mapping));
    }

    public InterfaceClassSet<P> mapChangeClassSet(DataSession Session, ClassSet ReqValue) {
        return Session.getChange(property).classes.getClassSet(ReqValue).map(mapping);
    }

    public ValueClassSet<P> mapValueClassSet(DataSession Session) {
        return Session.getChange(property).classes.getValueClassSet().map(mapping);
    }
}
