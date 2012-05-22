package platform.server.logics.property.actions;

import platform.base.BaseUtils;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;
import platform.server.logics.property.*;
import platform.server.session.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ActionEvent<P extends PropertyInterface> extends Event<P, ActionProperty<P>> {

    public ActionEvent(ActionProperty<P> writeTo, CalcPropertyMapImplement<?, P> where, int options) {
        super(writeTo, where);
        this.options = options;
    }

    public final static int RESOLVE = 1; // обозначает что where - SET или DROP свойства, и выполнение этого event'а не имеет смысла
    private final int options;

    public <T extends PropertyInterface> void resolve(DataSession session) throws SQLException {
        if((options & RESOLVE)==0)
            return;

        PropertyChanges changes = session.getPropertyChanges();
        for(ChangedProperty<T> changedProperty : where.property.getChangedDepends())
            changes = changes.add(new PropertyChanges(changedProperty, changedProperty.getFullChange(session)));
        new ExecutionEnvironment(session).execute(writeTo, getChange(changes), null);
    }

    public PropertySet<P> getChange(PropertyChanges changes) {
        Map<P,KeyExpr> mapKeys = writeTo.getMapKeys();
        return new PropertySet<P>(new HashMap<P, DataObject>(), mapKeys, where.mapExpr(mapKeys, changes).getWhere());
    }
}
