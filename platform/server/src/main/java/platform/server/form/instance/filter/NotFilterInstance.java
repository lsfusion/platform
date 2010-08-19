package platform.server.form.instance.filter;

import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.property.Property;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.form.instance.FormInstance;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class NotFilterInstance extends FilterInstance {

    FilterInstance filter;

    public NotFilterInstance(FilterInstance filter) {
        this.filter = filter;
    }

    protected NotFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        super(inStream, form);
        filter = deserialize(inStream, form);
    }

    public boolean classUpdated(GroupObjectInstance classGroup) {
        return filter.classUpdated(classGroup);
    }

    public boolean objectUpdated(GroupObjectInstance classGroup) {
        return filter.objectUpdated(classGroup);
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return filter.dataUpdated(changedProps);
    }

    public void fillProperties(Set<Property> properties) {
        filter.fillProperties(properties);
    }

    public GroupObjectInstance getApplyObject() {
        return filter.getApplyObject();
    }

    public Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) throws SQLException {
        return filter.getWhere(mapKeys, modifier).not();
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return filter.isInInterface(classGroup);
    }
}
