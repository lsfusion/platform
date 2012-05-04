package platform.server.form.instance.filter;

import platform.server.classes.CustomClass;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.PropertyObjectInstance;
import platform.server.logics.property.IsClassProperty;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Map;

public class IsClassFilterInstance<P extends PropertyInterface> extends PropertyFilterInstance<P> {

    final CustomClass isClass;

    public IsClassFilterInstance(PropertyObjectInstance<P> property, CustomClass isClass, boolean resolveAdd) {
        super(property, resolveAdd);
        this.isClass = isClass;
    }

    public IsClassFilterInstance(DataInputStream inStream, FormInstance form) throws IOException {
        super(inStream, form);
        isClass = form.getCustomClass(inStream.readInt());
    }

    public Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier modifier) {
        return IsClassProperty.getWhere(isClass, property.getExpr(mapKeys, modifier), modifier);
    }
}
