package platform.server.logics.constraints;

import java.sql.SQLException;

import platform.server.logics.session.DataSession;
import platform.server.logics.properties.Property;

/**
 *
 * @author ME2
 */

// constraint
public abstract class Constraint {

    public abstract String check(DataSession session, Property property) throws SQLException;

}
