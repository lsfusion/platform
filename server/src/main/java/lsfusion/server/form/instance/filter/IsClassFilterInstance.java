package lsfusion.server.form.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.Where;
import lsfusion.server.form.instance.CalcPropertyObjectInstance;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.ReallyChanged;
import lsfusion.server.logics.property.IsClassProperty;
import lsfusion.server.logics.property.PropertyInterface;
import lsfusion.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;

public class IsClassFilterInstance<P extends PropertyInterface> extends PropertyFilterInstance<P> {

    final CustomClass isClass;

    public IsClassFilterInstance(CalcPropertyObjectInstance<P> property, CustomClass isClass, boolean resolveAdd) {
        super(property, resolveAdd);
        this.isClass = isClass;
    }

    public IsClassFilterInstance(DataInputStream inStream, FormInstance form) throws IOException {
        super(inStream, form);
        isClass = form.getCustomClass(inStream.readInt());
    }

    public Where getWhere(ImMap<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier, ReallyChanged reallyChanged) {
        return IsClassProperty.getWhere(isClass, property.getExpr(mapKeys, modifier), modifier);
    }
}
