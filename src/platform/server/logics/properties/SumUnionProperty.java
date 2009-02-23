package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.data.query.Union;

public class SumUnionProperty extends UnionProperty {

    public SumUnionProperty(TableFactory iTableFactory) {super(iTableFactory, Union.SUM);}

    Integer getUnionType() {
        return 1;
    }
}
