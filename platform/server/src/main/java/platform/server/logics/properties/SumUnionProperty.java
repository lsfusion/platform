package platform.server.logics.properties;

import platform.server.data.Union;
import platform.server.logics.data.TableFactory;

import java.util.Collection;

public class SumUnionProperty extends UnionProperty {

    public SumUnionProperty(String iSID, int intNum, TableFactory iTableFactory) {
        super(iSID, intNum, iTableFactory, Union.SUM);
    }

    Integer getUnionType() {
        return 1;
    }
}
