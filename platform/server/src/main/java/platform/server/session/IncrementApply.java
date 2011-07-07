package platform.server.session;

import platform.base.BaseUtils;
import platform.server.classes.BaseClass;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.type.Type;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.table.ImplementTable;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.crossJoin;

// вообщем то public потому как иначе aspect не ловит
public class IncrementApply extends IncrementProps<KeyField> {

    public IncrementApply(DataSession session) {
        super(session);
    }

    public SessionTableUsage<KeyField, Property> read(final ImplementTable implement, Collection<Property> properties, BaseClass baseClass) throws SQLException {
        if (properties.size() == 1) {
            PropertyGroup<KeyField> changeTable = incrementGroups.get(BaseUtils.single(properties));
            if (changeTable != null) {
                return tables.get(changeTable);
            }
        }
        PropertyGroup<KeyField> groupTable = new PropertyGroup<KeyField>() {
            public List<KeyField> getKeys() {
                return implement.keys;
            }

            public Type.Getter<KeyField> typeGetter() {
                return Field.typeGetter();
            }

            public <P extends PropertyInterface> Map<P, KeyField> getPropertyMap(Property<P> property) {
                return property.mapTable.mapKeys;
            }
        };
        read(groupTable, properties, baseClass);
        return tables.get(groupTable);
    }

    public Map<ImplementTable, Collection<Property>> groupPropertiesByTables() {
       return BaseUtils.group(
                new BaseUtils.Group<ImplementTable, Property>() {
                    public ImplementTable group(Property key) {
                        return key.mapTable.table;
                    }
                }, incrementGroups.keySet());
    }
}
