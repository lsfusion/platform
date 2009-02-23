package platform.server.logics.session;

import platform.server.data.types.Type;

public class DataChangeTable extends ChangeObjectTable {

    public DataChangeTable(Integer iObjects, Type iDBType) {
        super("data",iObjects,iDBType);
    }
}
