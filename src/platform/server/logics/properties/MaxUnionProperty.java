package platform.server.logics.properties;

import platform.server.data.query.Union;
import platform.server.logics.data.TableFactory;

public class MaxUnionProperty extends UnionProperty {

    public MaxUnionProperty(TableFactory iTableFactory) {super(iTableFactory, Union.MAX);}

}
