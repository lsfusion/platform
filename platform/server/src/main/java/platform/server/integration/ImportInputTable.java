package platform.server.integration;

/**
 * User: DAle
 * Date: 24.02.11
 * Time: 15:38
 */

public interface ImportInputTable {
    public String getCellString(int row, int column);
    public int rowsCnt();
    public int columnsCnt();
}
