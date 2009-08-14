package platform.server.view.form.filter;

import platform.server.data.query.exprs.KeyExpr;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.session.TableChanges;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.RemoteForm;
import platform.server.where.Where;

import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.io.DataInputStream;
import java.io.IOException;

public class NotNullFilter<P extends PropertyInterface> extends PropertyFilter<P> {

    public NotNullFilter(PropertyObjectImplement<P> iProperty) {
        super(iProperty);
    }

    public NotNullFilter(DataInputStream inStream, RemoteForm form) throws IOException {
        super(inStream, form);
    }

    public Where getWhere(Map<ObjectImplement, KeyExpr> mapKeys, Set<GroupObjectImplement> classGroup, TableChanges session, Property.TableDepends<? extends Property.TableUsedChanges> depends) throws SQLException {
        return property.getSourceExpr(classGroup, mapKeys, session, depends).getWhere();
    }
}
