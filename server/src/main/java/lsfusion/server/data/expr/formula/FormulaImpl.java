package lsfusion.server.data.expr.formula;

import lsfusion.server.classes.ConcreteClass;
import lsfusion.server.data.expr.KeyType;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.type.Type;

public interface FormulaImpl {
    Type getType(ExprSource source, KeyType keyType);

    ConcreteClass getStaticClass(ExprSource source);

    String getSource(CompileSource compile, ExprSource source);
}
