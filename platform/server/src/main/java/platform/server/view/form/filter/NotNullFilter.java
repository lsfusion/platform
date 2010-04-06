package platform.server.view.form.filter;

import platform.server.data.expr.Expr;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.RemoteForm;
import platform.server.data.where.Where;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public class NotNullFilter<P extends PropertyInterface> extends PropertyFilter<P> {

    public NotNullFilter(PropertyObjectImplement<P> iProperty) {
        super(iProperty);
    }

    public NotNullFilter(DataInputStream inStream, RemoteForm form) throws IOException {
        super(inStream, form);
    }

    public Where getWhere(Map<ObjectImplement, ? extends Expr> mapKeys, Set<GroupObjectImplement> classGroup, Modifier<? extends Changes> modifier) throws SQLException {
        return property.getExpr(classGroup, mapKeys, modifier).getWhere();
    }
}
