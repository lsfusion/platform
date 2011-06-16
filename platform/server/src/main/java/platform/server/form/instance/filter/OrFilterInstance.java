package platform.server.form.instance.filter;

import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.form.entity.AbstractClassFormEntity;
import platform.server.form.instance.CustomObjectInstance;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.logics.DataObject;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.PropertyValueImplement;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class OrFilterInstance extends OpFilterInstance {

    public OrFilterInstance(FilterInstance op1, FilterInstance op2) {
        super(op1, op2);
    }

    protected OrFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        super(inStream, form);
    }

    public Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier) {
        return op1.getWhere(mapKeys, modifier).or(op2.getWhere(mapKeys, modifier));
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return op1.isInInterface(classGroup) || op2.isInInterface(classGroup);
    }
}
