package platform.server.logics.properties;

import platform.server.logics.classes.sets.InterfaceClassSet;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.logics.classes.sets.InterfaceClass;
import platform.server.logics.session.DataSession;
import platform.server.logics.session.DataChanges;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.JoinExpr;

import java.util.Map;
import java.util.List;
import java.util.Collection;

public interface PropertyInterfaceImplement<P extends PropertyInterface> {

    public SourceExpr mapSourceExpr(Map<P, ? extends SourceExpr> JoinImplement, InterfaceClassSet<P> JoinClasses);
    public ClassSet mapValueClass(InterfaceClass<P> ClassImplement);
    public InterfaceClassSet<P> mapClassSet(ClassSet ReqValue);


    abstract boolean mapFillChangedList(List<Property> ChangedProperties, DataChanges Changes, Collection<Property> NoUpdate);

    // для increment'ного обновления
    public boolean mapHasChanges(DataSession Session);
    public JoinExpr mapChangeExpr(DataSession Session, Map<P, ? extends SourceExpr> JoinImplement, int Value);
    ClassSet mapChangeValueClass(DataSession Session, InterfaceClass<P> ClassImplement);
    InterfaceClassSet<P> mapChangeClassSet(DataSession Session, ClassSet ReqValue);
}
