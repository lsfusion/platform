package lsfusion.server.form.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.GroupObjectInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.ReallyChanged;
import lsfusion.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class AndFilterInstance extends OpFilterInstance {

    public AndFilterInstance(FilterInstance op1, FilterInstance op2) {
        super(op1, op2);
    }

    protected AndFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
        super(inStream, form);
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged) throws SQLException, SQLHandledException {
        return op1.getWhere(mapKeys, modifier, reallyChanged).and(op2.getWhere(mapKeys, modifier, reallyChanged));
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return op1.isInInterface(classGroup) && op2.isInInterface(classGroup);
    }

}
