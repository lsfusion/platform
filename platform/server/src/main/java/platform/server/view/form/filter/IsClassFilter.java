package platform.server.view.form.filter;

import platform.server.data.classes.CustomClass;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.session.DataSession;
import platform.server.session.TableChanges;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.PropertyObjectImplement;
import platform.server.view.form.RemoteForm;
import platform.server.where.Where;

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

    public Where getWhere(Map<ObjectImplement, KeyExpr> mapKeys, Set<GroupObjectImplement> classGroup, TableChanges session, Property.TableDepends<? extends Property.TableUsedChanges> depends) throws SQLException {
        return DataSession.getIsClassWhere(session,property.getSourceExpr(classGroup, mapKeys, session, depends),isClass,null);
    }
}
