package platform.server.integration;

import platform.server.classes.ConcreteCustomClass;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyImplement;
import platform.server.logics.property.PropertyInterface;

import java.util.Map;

/**
 * User: DAle
 * Date: 24.12.10
 * Time: 16:33
 */

public class ImportKey<P extends PropertyInterface> {
    private ConcreteCustomClass keyClass;
    private PropertyImplement<ImportField, P> property;

    public ImportKey(ConcreteCustomClass keyClass, PropertyImplement<ImportField, P> property) {
        this.keyClass = keyClass;
        this.property = property;
    }

    public ConcreteCustomClass getCustomClass() {
        return keyClass;
    }

    public Map<P, ImportField> getMapping() {
        return property.mapping;
    }

    public Property<P> getProperty() {
        return property.property;
    }
}
