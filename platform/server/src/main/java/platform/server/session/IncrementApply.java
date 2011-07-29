package platform.server.session;

import platform.base.BaseUtils;
import platform.server.Settings;
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

    // assert что в properties содержатся
    public SessionTableUsage<KeyField, Property> read(final ImplementTable implement, Collection<Property> properties, BaseClass baseClass) throws SQLException {
        if (properties.size() == 1) {
            PropertyGroup<KeyField> changeTable = incrementGroups.get(BaseUtils.single(properties));
            if (changeTable != null) {
                return tables.get(changeTable);
            }
        }
        final int split = Settings.instance.getSplitIncrementApply();
        if(properties.size() > split) { // если слишком много групп, разделим на несколько read'ов
            // вообще тут пока используется дополнительный assertion, но пока это не так важно
            final List<Property> propertyList = new ArrayList<Property>(properties);
            for(Collection<Property> groupProps : BaseUtils.<Integer, Property>group(new BaseUtils.Group<Integer, Property>() {
                    public Integer group(Property key) {
                        return propertyList.indexOf(key) / split;
                    }
                }, propertyList).values())
                read(implement, groupProps, baseClass);
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
