package platform.server.integration;

import platform.server.logics.DataObject;

/**
 * User: DAle
 * Date: 03.02.11
 * Time: 18:25
 */

public interface ImportFieldInterface {
    DataObject getDataObject(ImportTable.Row row);

}
