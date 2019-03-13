package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.change.ReallyChanged;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class OrFilterInstance extends OpFilterInstance {

    public OrFilterInstance(FilterInstance op1, FilterInstance op2) {
        super(op1, op2);
    }

    protected OrFilterInstance(DataInputStream inStream, FormInstance form) throws IOException, SQLException, SQLHandledException {
        super(inStream, form);
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, MSet<CalcProperty> mUsedProps) throws SQLException, SQLHandledException {
        return op1.getWhere(mapKeys, modifier, reallyChanged, mUsedProps).or(op2.getWhere(mapKeys, modifier, reallyChanged, mUsedProps));
    }

    public boolean isInInterface(GroupObjectInstance classGroup) {
        return op1.isInInterface(classGroup) || op2.isInInterface(classGroup);
    }
}
