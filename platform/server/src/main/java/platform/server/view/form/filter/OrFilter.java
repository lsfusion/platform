package platform.server.view.form.filter;

import platform.server.data.expr.Expr;
import platform.server.logics.property.Property;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.RemoteForm;
import platform.server.data.where.Where;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class OrFilter extends Filter {

    Filter op1;
    Filter op2;

    public OrFilter(Filter op1, Filter op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    protected OrFilter(DataInputStream inStream, RemoteForm form) throws IOException, SQLException {
        super(inStream, form);
        op1 = Filter.deserialize(inStream, form);
        op2 = Filter.deserialize(inStream, form);
    }

    public boolean classUpdated(GroupObjectImplement classGroup) {
        return op1.classUpdated(classGroup) || op2.classUpdated(classGroup);
    }

    public boolean objectUpdated(GroupObjectImplement classGroup) {
        return op1.objectUpdated(classGroup) || op2.objectUpdated(classGroup);
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return op1.dataUpdated(changedProps) || op2.dataUpdated(changedProps);
    }

    public void fillProperties(Set<Property> properties) {
        op1.fillProperties(properties);
        op2.fillProperties(properties);
    }

    public GroupObjectImplement getApplyObject() {
        GroupObjectImplement apply1 = op1.getApplyObject();
        GroupObjectImplement apply2 = op2.getApplyObject();
        if(apply1.order>apply2.order)
            return apply1;
        else
            return apply2;
    }

    public Where getWhere(Map<ObjectImplement, ? extends Expr> mapKeys, Set<GroupObjectImplement> classGroup, Modifier<? extends Changes> modifier) throws SQLException {
        return op1.getWhere(mapKeys, classGroup, modifier).or(op2.getWhere(mapKeys, classGroup, modifier));
    }

    public boolean isInInterface(GroupObjectImplement classGroup) {
        return op1.isInInterface(classGroup) || op2.isInInterface(classGroup);
    }
}
