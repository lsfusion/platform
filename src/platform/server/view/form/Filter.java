package platform.server.view.form;

import platform.server.logics.properties.PropertyInterface;
import platform.server.logics.properties.Property;
import platform.server.logics.session.DataSession;
import platform.server.logics.classes.sets.ClassSet;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.wheres.CompareWhere;

import java.util.Collection;
import java.util.Set;
import java.util.Collections;
import java.io.DataInputStream;
import java.io.IOException;

public class Filter<P extends PropertyInterface> {

    public static int EQUALS = CompareWhere.EQUALS;
    public static int GREATER = CompareWhere.GREATER;
    public static int LESS = CompareWhere.LESS;
    public static int GREATER_EQUALS = CompareWhere.GREATER_EQUALS;
    public static int LESS_EQUALS = CompareWhere.LESS_EQUALS;
    public static int NOT_EQUALS = CompareWhere.NOT_EQUALS;

    public PropertyObjectImplement<P> property;
    public ValueLink value;
    public int compare;

    public Filter(PropertyObjectImplement<P> iProperty,int iCompare,ValueLink iValue) {
        property =iProperty;
        compare = iCompare;
        value = iValue;
    }

    public Filter(DataInputStream inStream, RemoteForm form) throws IOException {
        property = form.getPropertyView(inStream.readInt()).view;
        compare = inStream.readInt();
        value = ValueLink.deserialize(inStream, form);
    }


    public GroupObjectImplement getApplyObject() {
        return property.getApplyObject();
    }

    boolean dataUpdated(Collection<Property> ChangedProps) {
        return ChangedProps.contains(property.property);
    }

    boolean IsInInterface(GroupObjectImplement ClassGroup) {
        ClassSet ValueClass = value.getValueClass(ClassGroup);
        if(ValueClass==null)
            return property.isInInterface(ClassGroup);
        else
            return property.getValueClass(ClassGroup).intersect(ValueClass);
    }

    boolean ClassUpdated(GroupObjectImplement ClassGroup) {
        return property.classUpdated(ClassGroup) || value.ClassUpdated(ClassGroup);
    }

    boolean objectUpdated(GroupObjectImplement ClassGroup) {
        return property.objectUpdated(ClassGroup) || value.ObjectUpdated(ClassGroup);
    }

    void fillSelect(JoinQuery<ObjectImplement, ?> Query, Set<GroupObjectImplement> ClassGroup, DataSession Session) {
        Query.and(new CompareWhere(property.getSourceExpr(ClassGroup,Query.mapKeys,Session), value.getValueExpr(ClassGroup,Query.mapKeys,Session, property.property.getType()), compare));
    }

    public Collection<? extends Property> getProperties() {
        Collection<Property<P>> Result = Collections.singletonList(property.property);
        if(value instanceof PropertyValueLink)
            Result.add(((PropertyValueLink) value).property.property);
        return Result;
    }
}
