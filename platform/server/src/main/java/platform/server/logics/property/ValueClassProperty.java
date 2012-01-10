package platform.server.logics.property;

import platform.server.classes.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.where.Where;
import platform.server.session.DataSession;
import platform.server.logics.DataObject;

import java.sql.SQLException;
import java.util.Map;

public class ValueClassProperty extends ClassProperty<StaticClass> {

    final Object value;

    public ValueClassProperty(String sID, String caption, ValueClass[] classes, StaticClass staticClass, Object value) {
        super(sID, caption, classes, staticClass);
        
        this.value = value;

        assert value !=null;
    }

    protected Expr getStaticExpr() {
        return staticClass.getStaticExpr(value);
    }

    @Override
    protected void proceedNotNull(Map<ClassPropertyInterface, KeyExpr> mapKeys, Where where, DataSession session) throws SQLException {
        for(Map<ClassPropertyInterface, DataObject> row : new Query<ClassPropertyInterface, Object>(mapKeys, where).executeClasses(session.sql, session.env, session.baseClass).keySet())
            for(Map.Entry<ClassPropertyInterface, DataObject> entry : row.entrySet())
                if(entry.getKey().interfaceClass instanceof ConcreteObjectClass)
                    session.changeClass(entry.getValue(), (ConcreteObjectClass) entry.getKey().interfaceClass);
    }

    @Override
    protected void proceedNull(Map<ClassPropertyInterface, KeyExpr> mapKeys, Where where, DataSession session) throws SQLException {
        for(Map<ClassPropertyInterface, DataObject> row : new Query<ClassPropertyInterface, Object>(mapKeys, where).executeClasses(session.sql, session.env, session.baseClass).keySet())
            for(Map.Entry<ClassPropertyInterface, DataObject> entry : row.entrySet())
                session.changeClass(entry.getValue(), session.baseClass.unknown);
    }
}
