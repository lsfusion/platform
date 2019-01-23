package lsfusion.base.file;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public interface ExtraReadInterface {

    void copyToFile(String type, String query, File file) throws SQLException, IOException;

}