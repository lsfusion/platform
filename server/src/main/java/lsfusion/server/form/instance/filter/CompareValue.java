package lsfusion.server.form.instance.filter;

import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.form.instance.ObjectInstance;
import lsfusion.server.form.instance.Updated;
import lsfusion.server.session.Modifier;

public interface CompareValue extends Updated {

//    AndClassSet getValueClass(GroupObjectInstance ClassGroup) {return null;}

    Expr getExpr(ImMap<ObjectInstance, ? extends Expr> classSource, Modifier modifier);

    ImCol<ObjectInstance> getObjectInstances();
}
