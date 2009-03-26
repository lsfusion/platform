package platform.server.session;

import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.logics.classes.RemoteClass;

public class RemoveClassTable extends ChangeClassTable {

    public RemoveClassTable() {
        super("removechange");
    }

    public void excludeJoin(JoinQuery<?,?> query, DataSession session, RemoteClass changeClass, SourceExpr join) {
        Join<KeyField,PropertyField> classJoin = new Join<KeyField, PropertyField>(getClassJoin(session,changeClass));
        classJoin.joins.put(object,join);
        query.and(classJoin.inJoin.not());
    }

}
