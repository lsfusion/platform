package lsfusion.server.integration;

import java.text.ParseException;

/**
 * User: DAle
 * Date: 24.02.11
 * Time: 15:38
 */

public interface ImportInputTable {
    String getCellString(int row, int column);

    String getCellString(ImportField field, int row, int column) throws ParseException;

    int rowsCnt();

    int columnsCnt();


}
