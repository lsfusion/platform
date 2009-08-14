package platform.server.data.query.exprs;

import platform.server.data.classes.where.AndClassSet;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.where.DataWhereSet;

public abstract class VariableClassExpr extends AndExpr {

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return new ClassExprWhere(this,classes);
    }

    public abstract VariableClassExpr translateDirect(KeyTranslator translator);
}
