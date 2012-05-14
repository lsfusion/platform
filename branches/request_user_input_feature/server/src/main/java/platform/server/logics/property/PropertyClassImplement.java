package platform.server.logics.property;

import platform.base.BaseUtils;

import java.util.Collections;
import java.util.List;

/**
 * User: DAle
 * Date: 16.11.2010
 * Time: 14:36:18
 */

public class PropertyClassImplement<P extends PropertyInterface> extends PropertyImplement<P, ValueClassWrapper> {

    public PropertyClassImplement(Property<P> property, List<ValueClassWrapper> classes, List<P> interfaces) {
        super(property, BaseUtils.toMap(interfaces, classes));
    }
}
