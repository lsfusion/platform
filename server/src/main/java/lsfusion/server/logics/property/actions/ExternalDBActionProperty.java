package lsfusion.server.logics.property.actions;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.server.classes.DynamicFormatFileClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;

import java.io.IOException;
import java.sql.*;
import java.util.List;

public class ExternalDBActionProperty extends ExternalActionProperty {
    protected String exec;

    public ExternalDBActionProperty(int paramsCount, String connectionString, String exec, List<LCP> targetPropList) {
        super(paramsCount, connectionString, targetPropList);
        this.exec = exec;
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        Pair<String, String> replacedParams = replaceParams(context, connectionString, exec);
        byte[] result = readJDBC(replacedParams.first, replacedParams.second);

        for(LCP targetProp : targetPropList) {
            if (targetProp.property.getType() instanceof DynamicFormatFileClass) {
                targetProp.change(BaseUtils.mergeFileAndExtension(result, "jdbc".getBytes()), context);
            } else {
                targetProp.change(result, context);
            }
        }
    }

    private byte[] readJDBC(String connectionString, String exec) throws SQLException {
        Connection conn = null;

        try {
            conn = DriverManager.getConnection(connectionString);

            Statement statement = null;
            try {
                statement = conn.createStatement();
                ResultSet rs = statement.executeQuery(exec);

                return BaseUtils.serializeResultSet(rs);

            } finally {
                if (statement != null)
                    statement.close();
            }

        } catch (SQLException | IOException e) {
            throw Throwables.propagate(e);
        } finally {
            if (conn != null)
                conn.close();
        }
    }
}