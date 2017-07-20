package lsfusion.server.data.expr.formula;

import lsfusion.server.data.type.Type;

public interface FormulaImpl {
    Type getType(ExprType source);

    String getSource(ExprSource source);
}
