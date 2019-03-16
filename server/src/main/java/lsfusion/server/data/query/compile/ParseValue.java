package lsfusion.server.data.query.compile;

import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.type.parse.ParseInterface;

public interface ParseValue {

    ParseInterface getParseInterface(QueryEnvironment env, EnsureTypeEnvironment typeEnv);

    boolean isAlwaysSafeString(); // should be consistent with ParseInterface.isAlwaysSafeString, hack for recursions

    FunctionType getFunctionType();
}
