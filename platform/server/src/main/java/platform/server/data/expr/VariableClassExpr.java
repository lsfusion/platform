package platform.server.data.expr;

import platform.server.classes.sets.AndClassSet;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.translator.KeyTranslator;

public abstract class VariableClassExpr extends BaseExpr {

    public ClassExprWhere getClassWhere(AndClassSet classes) {
        return new ClassExprWhere(this,classes);
    }

    public abstract VariableClassExpr translateDirect(KeyTranslator translator);
}
