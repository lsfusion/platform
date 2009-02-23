package platform.server.view.form;

import java.util.Set;
import java.util.Map;

import platform.server.logics.session.DataSession;
import platform.server.data.types.Type;
import platform.server.data.query.exprs.SourceExpr;

public class UserValueLink extends ValueLink {

    public Object value;

    public UserValueLink(Object iValue) {
        value =iValue;
    }

    public SourceExpr getValueExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource, DataSession Session, Type DBType) {
        return DBType.getExpr(value);
    }
}
