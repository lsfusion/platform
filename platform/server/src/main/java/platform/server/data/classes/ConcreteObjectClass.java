package platform.server.data.classes;

import platform.server.data.classes.where.ObjectClassSet;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.DataObject;
import platform.server.session.SQLSession;

import java.sql.SQLException;
import java.util.Collection;

public interface ConcreteObjectClass extends ConcreteClass,ObjectClass,ObjectClassSet {

    public abstract void getDiffSet(ConcreteObjectClass diffClass, Collection<CustomClass> addClasses,Collection<CustomClass> removeClasses);

    public abstract SourceExpr getIDExpr();

    public abstract void saveClassChanges(SQLSession session, DataObject value) throws SQLException;
}
