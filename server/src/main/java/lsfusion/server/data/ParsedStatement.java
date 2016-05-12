package lsfusion.server.data;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.server.data.query.StaticExecuteEnvironment;

import java.sql.PreparedStatement;

/**
* Created by User on 27.02.2015.
*/
public class ParsedStatement {
    public final PreparedStatement statement;
    public final ImList<String> preparedParams;

    public ParsedStatement(PreparedStatement statement, ImList<String> preparedParams) {
        this.statement = statement;
        this.preparedParams = preparedParams;
    }
}
