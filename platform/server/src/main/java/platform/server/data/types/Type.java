package platform.server.data.types;

import platform.server.data.sql.SQLSyntax;
import platform.server.logics.DataObject;
import platform.server.view.form.client.report.ReportDrawField;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.Format;

public interface Type<T> extends Reader<T> {

    String getDB(SQLSyntax syntax);

    String getEmptyString();

    boolean isSafeString(Object value);
    String getString(Object value, SQLSyntax syntax);

    void writeParam(PreparedStatement statement, int num, Object value) throws SQLException;

    DataObject getEmptyValueExpr();

    Format getDefaultFormat();
    int getMinimumWidth();
    int getPreferredWidth();
    int getMaximumWidth();
    void fillReportDrawField(ReportDrawField reportField);

    boolean isCompatible(Type type);
}
