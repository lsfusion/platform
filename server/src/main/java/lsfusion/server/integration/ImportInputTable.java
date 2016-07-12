package lsfusion.server.integration;

import java.text.ParseException;

/**
 * User: DAle
 * Date: 24.02.11
 * Time: 15:38
 */

public interface ImportInputTable {
    public String getCellString(int row, int column);

    public String getCellString(ImportField field, int row, int column) throws ParseException;

    public int rowsCnt();

    public int columnsCnt();


}
