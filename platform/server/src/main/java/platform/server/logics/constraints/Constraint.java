package platform.server.logics.constraints;

import platform.server.logics.properties.Property;
import platform.server.session.DataSession;

import java.sql.SQLException;

/**
 *
 * @author ME2
 */

// constraint
public abstract class Constraint {

    public abstract String check(DataSession session, Property property) throws SQLException;

}
