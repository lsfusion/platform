package platform.server.logics.properties;

import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.session.DataChanges;
import platform.server.logics.session.DataSession;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PropertyInterfaceImplement<P extends PropertyInterface> {

    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> joinImplement, InterfaceClassSet<P> joinClasses);
    public ClassSet mapValueClass(InterfaceClass<P> classImplement);
    public InterfaceClassSet<P> mapClassSet(ClassSet reqValue);


    abstract boolean mapFillChangedList(List<Property> changedProperties, DataChanges changes, Collection<Property> noUpdate);

    // для increment'ного обновления
    public boolean mapHasChanges(DataSession session);
    public JoinExpr mapChangeExpr(DataSession session, Map<P, ? extends SourceExpr> joinImplement, int value);
    ClassSet mapChangeValueClass(DataSession session, InterfaceClass<P> classImplement);
    InterfaceClassSet<P> mapChangeClassSet(DataSession session, ClassSet reqValue);
}
