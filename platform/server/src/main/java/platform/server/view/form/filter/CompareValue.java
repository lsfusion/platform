package platform.server.view.form.filter;

import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.logics.properties.Property;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.session.TableChanges;

import java.util.Collection;
import java.util.Set;
import java.util.Map;
import java.sql.SQLException;

public interface CompareValue {

//    AndClassSet getValueClass(GroupObjectImplement ClassGroup) {return null;}

    boolean classUpdated(GroupObjectImplement classGroup);
    boolean objectUpdated(GroupObjectImplement classGroup);
    boolean dataUpdated(Collection<Property> changedProps);
    void fillProperties(Collection<Property> properties);

    SourceExpr getSourceExpr(Set<GroupObjectImplement> classGroup, Map<ObjectImplement, ? extends SourceExpr> classSource, TableChanges session, Property.TableDepends<? extends Property.TableUsedChanges> depends) throws SQLException;
}
