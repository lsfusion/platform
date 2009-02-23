package platform.server.logics.properties;

import platform.server.logics.data.TableFactory;
import platform.server.data.query.Union;

public class OverrideUnionProperty extends UnionProperty {

    public OverrideUnionProperty(TableFactory iTableFactory) {super(iTableFactory, Union.OVERRIDE);}
}
