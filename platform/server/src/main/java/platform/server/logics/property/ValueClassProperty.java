package platform.server.logics.property;

import platform.server.classes.ConcreteValueClass;
import platform.server.classes.ValueClass;
import platform.server.classes.StaticClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.session.SimpleChanges;
import platform.server.logics.DataObject;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ValueClassProperty extends ClassProperty<StaticClass> {

    final Object value;

    public ValueClassProperty(String sID, String caption, ValueClass[] classes, StaticClass staticClass, Object value) {
        super(sID, caption, classes, staticClass);
        
        this.value = value;

        assert value !=null;
    }

    protected Expr getStaticExpr() {
        return staticClass.getStaticExpr(value);
    }
}
