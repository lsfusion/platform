package lsfusion.server.data;

import lsfusion.server.data.type.FunctionType;
import lsfusion.server.data.type.ParseInterface;

public interface ParseValue {

    ParseInterface getParseInterface();

    FunctionType getFunctionType();
}
