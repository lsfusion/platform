package platform.server.form.instance.filter;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class OrFilterInstance extends OpFilterInstance {

    public OrFilterInstance(FilterInstance op1, FilterInstance op2) {
        super(op1, op2);
    }

    protected OrFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        super(inStream, form);
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        return op1.getWhere(mapKeys, modifier).or(op2.getWhere(mapKeys, modifier));
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return op1.isInInterface(classGroup) || op2.isInInterface(classGroup);
    }
}
