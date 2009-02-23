package platform.server.logics.properties;

import java.util.Map;
import java.util.List;
import java.util.Collection;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.session.DataSession;
import platform.server.logics.session.DataChanges;

public class PropertyInterface<P extends PropertyInterface<P>> implements PropertyInterfaceImplement<P> {

    int ID = 0;
    public PropertyInterface(int iID) {
        ID = iID;
    }

    public String toString() {
        return "I/"+ID;
    }

    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses) {
        return JoinImplement.get(this);
    }

    public JoinExpr mapChangeExpr(DataSession Session, Map<P, ? extends SourceExpr> JoinImplement, int Value) {
        return null;
    }

    public ClassSet mapValueClass(InterfaceClass<P> ClassImplement) {
        return ClassImplement.get(this);
    }

    public InterfaceClassSet<P> mapClassSet(ClassSet ReqValue) {
        InterfaceClass<P> ResultClass = new InterfaceClass<P>();
        ResultClass.put((P) this,ReqValue);
        return new InterfaceClassSet<P>(ResultClass);
    }

    public boolean mapHasChanges(DataSession Session) {
        return false;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate) {
        return false;
    }

    public ClassSet mapChangeValueClass(DataSession Session, InterfaceClass<P> ClassImplement) {
        return mapValueClass(ClassImplement);
    }

    public InterfaceClassSet<P> mapChangeClassSet(DataSession Session, ClassSet ReqValue) {
        return mapClassSet(ReqValue);
    }
}
