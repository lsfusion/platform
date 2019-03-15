package lsfusion.server.data.expr.formula.conversion;

import lsfusion.server.data.query.MStaticExecuteEnvironment;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.logics.classes.data.DataClass;

public interface ConversionSource {
    String getSource(DataClass type1, DataClass type2, String src1, String src2, SQLSyntax syntax, MStaticExecuteEnvironment env, boolean isToString);
}
