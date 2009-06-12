package platform.server.view.form;

import platform.interop.Compare;
import platform.server.data.query.JoinQuery;
import platform.server.logics.properties.*;
import platform.server.session.TableChanges;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class Filter<P extends PropertyInterface> {

    public final static int EQUALS = Compare.EQUALS;
    public final static int GREATER = Compare.GREATER;
    public final static int LESS = Compare.LESS;
    public final static int GREATER_EQUALS = Compare.GREATER_EQUALS;
    public final static int LESS_EQUALS = Compare.LESS_EQUALS;
    public final static int NOT_EQUALS = Compare.NOT_EQUALS;

    public PropertyObjectImplement<P> property;
    public ValueLink value;
    public int compare;

    public Filter(PropertyObjectImplement<P> iProperty,int iCompare,ValueLink iValue) {
        property =iProperty;
        compare = iCompare;
        value = iValue;
    }

    public Filter(DataInputStream inStream, RemoteForm form) throws IOException, SQLException {
        property = form.getPropertyView(inStream.readInt()).view;
        compare = inStream.readInt();
        value = ValueLink.deserialize(inStream, form, property.property.getType());
    }


    public GroupObjectImplement getApplyObject() {
        return property.getApplyObject();
    }

    boolean dataUpdated(Collection<Property> ChangedProps) {
        return ChangedProps.contains(property.property);
    }

    boolean isInInterface(GroupObjectImplement classGroup) {
        return true; // пока будем считать что нету в интерфейсе и включать тоже не будем
        /* ClassSet valueClass = value.getValueClass(classGroup);
        if(valueClass==null)
            return property.isInInterface(classGroup);
        else
            return property.getValueClass(classGroup).intersect(valueClass); */
    }

    boolean classUpdated(GroupObjectImplement ClassGroup) {
        return property.classUpdated(ClassGroup) || value.classUpdated(ClassGroup);
    }

    boolean objectUpdated(GroupObjectImplement ClassGroup) {
        return property.objectUpdated(ClassGroup) || value.objectUpdated(ClassGroup);
    }

    void fillSelect(JoinQuery<ObjectImplement, ?> query, Set<GroupObjectImplement> classGroup, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException {
        query.and(property.getSourceExpr(classGroup, query.mapKeys, session, defaultProps, noUpdateProps).compare(value.getValueExpr(classGroup, query.mapKeys, session, property.property.getType(), defaultProps, noUpdateProps), compare));
    }

    public Collection<? extends Property> getProperties() {
        Collection<Property<P>> Result = Collections.singletonList(property.property);
        if(value instanceof PropertyValueLink)
            Result.add(((PropertyValueLink) value).property.property);
        return Result;
    }
}
