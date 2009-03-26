package platform.server.logics.properties;

import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.session.DataChanges;
import platform.server.session.DataSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PropertyInterface<P extends PropertyInterface<P>> implements PropertyInterfaceImplement<P> {

    public int ID = 0;
    public PropertyInterface(int iID) {
        ID = iID;
    }

    public String toString() {
        return "I/"+ID;
    }

    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> joinImplement, InterfaceClassSet<P> joinClasses) {
        return joinImplement.get(this);
    }

    public JoinExpr mapChangeExpr(DataSession session, Map<P, ? extends SourceExpr> joinImplement, int value) {
        return null;
    }

    public ClassSet mapValueClass(InterfaceClass<P> classImplement) {
        return classImplement.get(this);
    }

    public InterfaceClassSet<P> mapClassSet(ClassSet reqValue) {
        InterfaceClass<P> ResultClass = new InterfaceClass<P>();
        ResultClass.put((P) this, reqValue);
        return new InterfaceClassSet<P>(ResultClass);
    }

    public boolean mapHasChanges(DataSession session) {
        return false;
    }

    // заполняет список, возвращает есть ли изменения
    public boolean mapFillChangedList(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate) {
        return false;
    }

    public ClassSet mapChangeValueClass(DataSession session, InterfaceClass<P> classImplement) {
        return mapValueClass(classImplement);
    }

    public InterfaceClassSet<P> mapChangeClassSet(DataSession session, ClassSet reqValue) {
        return mapClassSet(reqValue);
    }
}
