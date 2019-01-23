package lsfusion.base.file;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public interface ExtraReadInterface {

    void copyJDBCToFile(String query, File file) throws SQLException;

    void copyMDBToFile(String query, File file) throws IOException;

}