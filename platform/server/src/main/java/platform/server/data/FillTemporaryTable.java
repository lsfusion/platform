package platform.server.data;

import java.sql.SQLException;
import java.util.List;

public interface FillTemporaryTable {

    Integer fill(String name) throws SQLException;
}
