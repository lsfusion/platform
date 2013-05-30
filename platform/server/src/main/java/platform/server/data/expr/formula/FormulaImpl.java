package platform.server.data.expr.formula;

import platform.server.classes.ConcreteClass;
import platform.server.data.expr.KeyType;
import platform.server.data.query.CompileSource;
import platform.server.data.type.Type;

public interface FormulaImpl {
    Type getType(ExprSource source, KeyType keyType);

    ConcreteClass getStaticClass(ExprSource source);

    String getSource(CompileSource compile, ExprSource source);
}
