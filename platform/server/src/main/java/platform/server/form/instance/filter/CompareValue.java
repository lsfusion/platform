package platform.server.form.instance.filter;

import platform.base.col.interfaces.immutable.ImCol;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImSet;
import platform.server.data.expr.Expr;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.Updated;
import platform.server.session.Modifier;

public interface CompareValue extends Updated {

//    AndClassSet getValueClass(GroupObjectInstance ClassGroup) {return null;}

    Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier);

    ImCol<ObjectInstance> getObjectInstances();
}
