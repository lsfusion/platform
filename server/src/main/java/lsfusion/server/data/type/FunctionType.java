package lsfusion.server.data.type;

import lsfusion.base.serialization.BinarySerializable;
import lsfusion.server.data.query.TypeEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;

public interface FunctionType extends BinarySerializable {
    String getDB(SQLSyntax syntax, TypeEnvironment typeEnv);

    String getParamFunctionDB(SQLSyntax syntax, TypeEnvironment typeEnv);
}
