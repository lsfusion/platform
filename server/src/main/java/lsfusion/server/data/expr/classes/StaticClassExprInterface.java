package lsfusion.server.data.expr.classes;

import lsfusion.server.data.expr.key.KeyType;
import lsfusion.server.logics.classes.ConcreteClass;

public interface StaticClassExprInterface {
    ConcreteClass getStaticClass();

    ConcreteClass getStaticClass(KeyType keyType);
}
