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

public class CompareFilter<P extends PropertyInterface> extends Filter<P> {

    public ValueLink value;
    public int compare;

    public CompareFilter(PropertyObjectImplement<P> iProperty,int iCompare,ValueLink iValue) {
        super(iProperty);
        compare = iCompare;
        value = iValue;
    }

    public CompareFilter(DataInputStream inStream, RemoteForm form) throws IOException, SQLException {
        super(inStream,form);
        compare = inStream.readInt();
        value = ValueLink.deserialize(inStream, form, property.property.getType());
    }


    boolean dataUpdated(Collection<Property> changedProps) {
        return super.dataUpdated(changedProps) || value.dataUpdated(changedProps);
    }

    boolean classUpdated(GroupObjectImplement classGroup) {
        return super.classUpdated(classGroup) || value.classUpdated(classGroup);
    }

    boolean objectUpdated(GroupObjectImplement classGroup) {
        return super.objectUpdated(classGroup) || value.objectUpdated(classGroup);
    }

    public void fillSelect(JoinQuery<ObjectImplement, ?> query, Set<GroupObjectImplement> classGroup, TableChanges session, Map<DataProperty, DefaultData> defaultProps, Collection<Property> noUpdateProps) throws SQLException {
        query.and(property.getSourceExpr(classGroup, query.mapKeys, session, defaultProps, noUpdateProps).compare(value.getValueExpr(classGroup, query.mapKeys, session, property.property.getType(), defaultProps, noUpdateProps), compare));
    }

    @Override
    protected void fillProperties(Collection<Property> properties) {
        super.fillProperties(properties);
        value.fillProperties(properties);
    }
}
