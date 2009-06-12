package platform.server.logics.constraints;

import platform.server.logics.properties.DataProperty;
import platform.server.logics.properties.DefaultData;
import platform.server.logics.properties.Property;
import platform.server.logics.properties.PropertyInterface;
import platform.server.session.DataSession;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

/**
 *
 * @author ME2
 */

// constraint
public abstract class Constraint {

    public abstract <P extends PropertyInterface> String check(DataSession session, Property<P> property, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException;

}
