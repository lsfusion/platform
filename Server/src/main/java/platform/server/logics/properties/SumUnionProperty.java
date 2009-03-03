package platform.server.logics.properties;

import platform.server.data.Union;
import platform.server.logics.data.TableFactory;

public class SumUnionProperty extends UnionProperty {

    public SumUnionProperty(TableFactory iTableFactory) {super(iTableFactory, Union.SUM);}

    Integer getUnionType() {
        return 1;
    }
}
