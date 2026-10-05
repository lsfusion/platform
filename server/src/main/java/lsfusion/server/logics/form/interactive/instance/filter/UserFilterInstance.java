package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.interop.form.property.Compare;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.value.DataObject;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.PropertyColumn;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// a filter the user sets on a group, kept as it was set: a condition on a property in one of its columns; and the
// filter that condition is applied as
public class UserFilterInstance extends TwinImmutableObject {

    public final PropertyColumn column;
    public final boolean negation;
    public final Compare compare;
    public final ObjectValue value;
    public final boolean junction; //true - conjunction, false - disjunction

    public final FilterInstance filter;

    public UserFilterInstance(PropertyColumn column, boolean negation, Compare compare, ObjectValue value, boolean junction) {
        this.column = column;
        this.negation = negation;
        this.compare = compare;
        this.value = value;
        this.junction = junction;

        filter = createFilter(column.property);
        filter.junction = junction;
    }

    // as a client sends it (ClientPropertyFilter.serialize)
    public static UserFilterInstance deserialize(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
        PropertyDrawInstance<?> property = form.getPropertyDraw(inStream.readInt());
        // no column keys: the column of the current objects of the column groups
        ImMap<ObjectInstance, DataObject> columnKeys = MapFact.EMPTY();
        if(inStream.readBoolean())
            columnKeys = DataObject.assertDataObjects(RemoteForm.deserializeKeysValues(inStream, form));
        boolean negation = inStream.readBoolean();
        Compare compare = Compare.deserialize(inStream);
        ObjectValue value = form.session.getObjectValue(property.getFilterProperty().getFilterValueClass(compare), BaseUtils.deserializeObject(inStream));
        boolean junction = inStream.readBoolean();
        return new UserFilterInstance(new PropertyColumn(property, columnKeys), negation, compare, value, junction);
    }

    private <P extends PropertyInterface> FilterInstance createFilter(PropertyDrawInstance<P> propertyDraw) {
        PropertyObjectInstance<P> property = ((PropertyObjectInstance<P>) propertyDraw.getFilterProperty()).getRemappedPropertyObject(column.columnKeys, false);
        GroupObjectInstance toDraw = propertyDraw.toDraw;

        if (value instanceof NullValue) {
            FilterInstance notNullFilter = new NotNullFilterInstance<>(property, toDraw);
            return negation ? notNullFilter : new NotFilterInstance(notNullFilter);
        }

        DataObject dataValue = (DataObject) value;
        if (dataValue.objectClass instanceof StringClass) {
            boolean isContains = compare == Compare.CONTAINS;
            boolean isEquals = compare == Compare.EQUALS;

            String filterValue = (String) dataValue.object;

            String separator = Settings.get().getMatchSearchSeparator();
            if ((isContains || isEquals) && (filterValue.contains(separator))) {
                FilterInstance resultFilter = null;
                //one or more repetitions of \ and then any one char, or any char but \ and separator
                Matcher matcher = Pattern.compile("(?:\\\\.|[^\\\\" + separator + "])+", Pattern.DOTALL).matcher(filterValue);
                while (matcher.find()) {
                    String value = matcher.group().replace("\\" + separator, separator); //unescape escaped separator
                    if (FilterInstance.needWrapContains(value, isContains))
                        value = FilterInstance.wrapContains(value);
                    CompareFilterInstance<P> filterInstance = new CompareFilterInstance<>(property, false, toDraw, negation, compare,
                            new DataObject(value, dataValue.objectClass));

                    resultFilter = resultFilter == null ? filterInstance : new OrFilterInstance(resultFilter, filterInstance);
                }
                if (resultFilter != null)
                    return resultFilter;
            } else if (FilterInstance.needWrapContains(filterValue, isContains))
                return new CompareFilterInstance<>(property, false, toDraw, negation, compare,
                        new DataObject(FilterInstance.wrapContains(filterValue), dataValue.objectClass));
        }
        return new CompareFilterInstance<>(property, false, toDraw, negation, compare, value);
    }

    protected boolean calcTwins(TwinImmutableObject o) {
        UserFilterInstance userFilter = (UserFilterInstance) o;
        return column.equals(userFilter.column) && negation == userFilter.negation && compare == userFilter.compare &&
                value.equals(userFilter.value) && junction == userFilter.junction;
    }

    public int immutableHashCode() {
        return (((column.hashCode() * 31 + (negation ? 1 : 0)) * 31 + compare.hashCode()) * 31 + value.hashCode()) * 31 +
                (junction ? 1 : 0);
    }
}
