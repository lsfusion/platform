package platform.server.view.form.filter;

import platform.server.classes.CustomClass;
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

public class IsClassFilter<P extends PropertyInterface> extends PropertyFilter<P> {

    final CustomClass isClass;

    public IsClassFilter(PropertyObjectImplement<P> iProperty, CustomClass isClass) {
        super(iProperty);
        this.isClass = isClass;
    }

    public IsClassFilter(DataInputStream inStream, RemoteForm form) throws IOException {
        super(inStream, form);
        isClass = form.getCustomClass(inStream.readInt());
    }

    public Where getWhere(Map<ObjectImplement, ? extends Expr> mapKeys, Set<GroupObjectImplement> classGroup, Modifier<? extends Changes> modifier) throws SQLException {
        return modifier.getSession().getIsClassWhere(property.getExpr(classGroup, mapKeys, modifier), isClass, null);
    }
}
