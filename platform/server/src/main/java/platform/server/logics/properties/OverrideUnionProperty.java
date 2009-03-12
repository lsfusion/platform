package platform.server.logics.properties;

import platform.server.data.Union;
import platform.server.logics.data.TableFactory;

import java.util.Collection;

public class OverrideUnionProperty extends UnionProperty {

    public OverrideUnionProperty(String iSID, int intNum, TableFactory iTableFactory) {
        super(iSID, intNum, iTableFactory, Union.OVERRIDE);
    }
}
