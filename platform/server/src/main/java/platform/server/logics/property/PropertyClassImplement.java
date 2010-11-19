package platform.server.logics.property;

import java.util.HashMap;
import java.util.List;

/**
 * User: DAle
 * Date: 16.11.2010
 * Time: 14:36:18
 */

public class PropertyClassImplement<P extends PropertyInterface> extends PropertyImplement<ValueClassWrapper, P> {
    public PropertyClassImplement(Property<P> property, List<ValueClassWrapper> classes, List<P> interfaces) {
        assert classes.size() == interfaces.size();
        this.property = property;
        this.mapping = new HashMap<P, ValueClassWrapper>();
        for (int i = 0; i < classes.size(); i++) {
            this.mapping.put(interfaces.get(i), classes.get(i));
        }
    }

    public PropertyClassImplement(Property<P> property, ValueClassWrapper vClass, P iFace) {
        this.property = property;
        this.mapping = new HashMap<P, ValueClassWrapper>();
        this.mapping.put(iFace, vClass);
    }
}
