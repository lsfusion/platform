package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.classes.DataClass;
import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.query.StaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;

public interface ConversionSource {
    String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env);
}
