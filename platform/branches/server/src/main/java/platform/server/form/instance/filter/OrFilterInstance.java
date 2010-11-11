package platform.server.form.instance.filter;

import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class OrFilterInstance extends FilterInstance {

    public FilterInstance op1;
    public FilterInstance op2;

    public OrFilterInstance(FilterInstance op1, FilterInstance op2) {
        this.op1 = op1;
        this.op2 = op2;
    }

    protected OrFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        super(inStream, form);
        op1 = deserialize(inStream, form);
        op2 = deserialize(inStream, form);
    }

    public boolean classUpdated(GroupObjectInstance classGroup) {
        return op1.classUpdated(classGroup) || op2.classUpdated(classGroup);
    }

    public boolean objectUpdated(Set<GroupObjectInstance> skipGroups) {
        return op1.objectUpdated(skipGroups) || op2.objectUpdated(skipGroups);
    }

    public boolean dataUpdated(Collection<Property> changedProps) {
        return op1.dataUpdated(changedProps) || op2.dataUpdated(changedProps);
    }

    public void fillProperties(Set<Property> properties) {
        op1.fillProperties(properties);
        op2.fillProperties(properties);
    }

    public GroupObjectInstance getApplyObject() {
        GroupObjectInstance apply1 = op1.getApplyObject();
        GroupObjectInstance apply2 = op2.getApplyObject();
        if(apply2==null || (apply1!=null && apply1.order>apply2.order))
            return apply1;
        else
            return apply2;
    }

    public Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) throws SQLException {
        return op1.getWhere(mapKeys, modifier).or(op2.getWhere(mapKeys, modifier));
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return op1.isInInterface(classGroup) || op2.isInInterface(classGroup);
    }

    @Override
    public void resolveAdd(DataSession session, Modifier<? extends Changes> modifier, CustomObjectInstance object, DataObject addObject) throws SQLException {
        op1.resolveAdd(session, modifier, object, addObject);
        op2.resolveAdd(session, modifier, object, addObject);
    }
}
