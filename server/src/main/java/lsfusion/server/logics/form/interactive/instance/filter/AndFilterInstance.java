package lsfusion.server.logics.form.interactive.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.form.interactive.changed.ReallyChanged;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.property.Property;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;

public class AndFilterInstance extends OpFilterInstance {

    public AndFilterInstance(FilterInstance op1, FilterInstance op2) {
        super(op1, op2);
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged, MSet<Property> mUsedProps) throws SQLException, SQLHandledException {
        return op1.getWhere(mapKeys, modifier, reallyChanged, mUsedProps).and(op2.getWhere(mapKeys, modifier, reallyChanged, mUsedProps));
    }
}
