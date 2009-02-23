package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.data.query.Union;

public class MaxUnionProperty extends UnionProperty {

    public MaxUnionProperty(TableFactory iTableFactory) {super(iTableFactory, Union.MAX);}

}
