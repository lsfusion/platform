package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.classes.DataClass;
import lsfusion.server.data.query.CompileSource;
import lsfusion.server.data.query.ExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.data.type.Type;

public interface ConversionSource {
    String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, ExecuteEnvironment env);
}
