package platform.server.view.form;

import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DefaultData;
import platform.server.logics.properties.Property;
import platform.server.data.query.JoinQuery;
import platform.server.session.TableChanges;

import java.util.Set;
import java.util.Map;
import java.util.Collection;
import java.sql.SQLException;

public class NotNullFilter<P extends PropertyInterface> extends Filter<P> {

    public NotNullFilter(PropertyObjectImplement<P> iProperty) {
        super(iProperty);
    }

    public void fillSelect(JoinQuery<ObjectImplement, ?> query, Set<GroupObjectImplement> classGroup, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException {
        query.and(property.getSourceExpr(classGroup, query.mapKeys, session, defaultProps, noUpdateProps).getWhere());
    }
}
