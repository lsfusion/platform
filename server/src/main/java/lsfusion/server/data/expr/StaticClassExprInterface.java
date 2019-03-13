package lsfusion.server.data.expr;

import lsfusion.server.logics.classes.ConcreteClass;

public interface StaticClassExprInterface {
    ConcreteClass getStaticClass();

    ConcreteClass getStaticClass(KeyType keyType);
}
