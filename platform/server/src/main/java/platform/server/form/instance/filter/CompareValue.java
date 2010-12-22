package platform.server.form.instance.filter;

import platform.server.data.expr.Expr;
import platform.server.form.instance.ObjectInstance;
import platform.server.form.instance.Updated;
import platform.server.session.Changes;
import platform.server.session.Modifier;

import java.util.Collection;
import java.util.Map;

public interface CompareValue extends Updated {

//    AndClassSet getValueClass(GroupObjectInstance ClassGroup) {return null;}

    Expr getExpr(Map<ObjectInstance, ? extends Expr> classSource, Modifier<? extends Changes> modifier);

    Collection<ObjectInstance> getObjectInstances();
}
