package lsfusion.server.data.expr.formula;

import lsfusion.server.data.type.Type;

public interface ExprType {

    boolean isParam(int i); // временный хак

    int getExprCount();

    Type getType(int i);

}
