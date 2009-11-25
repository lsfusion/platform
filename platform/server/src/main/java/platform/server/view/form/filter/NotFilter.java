package platform.server.view.form.filter;

import platform.server.view.form.RemoteForm;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.logics.property.Property;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.data.expr.KeyExpr;
import platform.server.data.where.Where;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Set;
import java.util.Map;

public class NotFilter extends Filter {

    Filter filter;

    public NotFilter(Filter filter) {
        this.filter = filter;
    }

    protected NotFilter(DataInputStream inStream, RemoteForm form) throws IOException, SQLException {
        super(inStream, form);
        filter = Filter.deserialize(inStream, form);
    }

    public boolean classUpdated(GroupObjectImplement classGroup) {
        return filter.classUpdated(classGroup);
    }

    public boolean objectUpdated(GroupObjectImplement classGroup) {
        return filter.objectUpdated(classGroup);
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return filter.dataUpdated(changedProps);
    }

    public void fillProperties(Set<Property> properties) {
        filter.fillProperties(properties);
    }

    public GroupObjectImplement getApplyObject() {
        return filter.getApplyObject();
    }

    public Where getWhere(Map<ObjectImplement, KeyExpr> mapKeys, Set<GroupObjectImplement> classGroup, TableModifier<? extends TableChanges> modifier) throws SQLException {
        return filter.getWhere(mapKeys, classGroup, modifier).not();
    }

    public boolean isInInterface(GroupObjectImplement classGroup) {
        return filter.isInInterface(classGroup);
    }
}
