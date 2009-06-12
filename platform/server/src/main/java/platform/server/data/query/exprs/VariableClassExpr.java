package platform.server.data.query.exprs;

import platform.server.data.classes.BaseClass;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.ConcreteObjectClass;
import platform.server.data.classes.where.*;
import platform.server.data.query.wheres.IsClassWhere;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.types.ObjectType;
import platform.server.where.DataWhereSet;
import platform.server.where.Where;

import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

public abstract class VariableClassExpr extends AndExpr {

    @Override
    public ClassExprWhere getClassWhere(ClassSet classes) {
        return new ClassExprWhere(this,classes);
    }

    public DataWhereSet getFollows() {
        return new DataWhereSet();
    }

    protected int getHash() {
        return 1;
    }
}
