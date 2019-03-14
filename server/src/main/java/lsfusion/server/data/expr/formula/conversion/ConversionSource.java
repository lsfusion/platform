package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.SQLSyntax;
import lsfusion.server.logics.classes.DataClass;

public interface ConversionSource {
    String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString);
}
