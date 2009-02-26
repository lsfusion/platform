package platform.server.logics.properties;

import platform.server.data.query.Union;
import platform.server.logics.data.TableFactory;

public class OverrideUnionProperty extends UnionProperty {

    public OverrideUnionProperty(TableFactory iTableFactory) {super(iTableFactory, Union.OVERRIDE);}
}
