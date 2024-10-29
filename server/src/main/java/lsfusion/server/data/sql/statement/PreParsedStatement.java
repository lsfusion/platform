package lsfusion.server.data.sql.statement;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.base.mutability.TwinImmutableObject;
import lsfusion.server.base.caches.IdentityLazy;
import lsfusion.server.data.expr.where.classes.data.BinaryWhere;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.syntax.SQLSyntax;
import lsfusion.server.data.type.ConcatenateType;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.type.exec.EnsureTypeEnvironment;
import lsfusion.server.data.type.exec.TypeEnvironment;
import lsfusion.server.data.type.parse.ParsedParamString;
import lsfusion.server.data.type.parse.ParsedString;
import lsfusion.server.physics.admin.Settings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PreParsedStatement extends TwinImmutableObject { // вообще StringParseInterfce, но множественного наследования нет
    public final String statement;
    public final ImSet<String> params;
    public final ImMap<String, ParsedString> safeStrings;
    public final ImMap<String, Type> notSafeTypes;
    public final boolean volatileStats;
    public final String envString;
    public final EnsureTypeEnvironment ensureTypes;

    public void checkSessionTable(SQLSession sql) {
    }

    public boolean isSafeString() {
        return true;
    }

    public void writeParam(PreparedStatement statement, SQLSession.ParamNum paramNum, SQLSyntax syntax, TypeEnvironment env) {
        throw new RuntimeException("not supported");
    }

    public boolean isSafeType() {
        return true;
    }

    public Type getType() {
        throw new RuntimeException("not supported");
    }


    public PreParsedStatement(String statement, ImSet<String> params, ImMap<String, ParsedString> safeStrings, ImMap<String, Type> notSafeTypes, boolean volatileStats, String envString, EnsureTypeEnvironment ensureTypes) {
        this.statement = statement;
        this.params = params;
        this.safeStrings = safeStrings;
        this.notSafeTypes = notSafeTypes;
        this.volatileStats = volatileStats;
        this.envString = envString;
        this.ensureTypes = ensureTypes;
    }

    @IdentityLazy
    public ParsedParamString getString(SQLSyntax syntax) {
        MList<String> mPreparedParams = ListFact.mList();

        char[][] paramArrays = new char[params.size()+1][];
        String[] aParams = new String[paramArrays.length];
        ParsedString[] aSafeStrings = new ParsedString[paramArrays.length];
        Type[] aNotSafeTypes = new Type[paramArrays.length];
        int paramNum = 0;

        boolean hasNoParamPrefix = false;
        for (int i=0,size= params.size();i<size;i++) {
            String param = params.get(i);
            paramArrays[paramNum] = param.toCharArray();
            aParams[paramNum] = param;
            aSafeStrings[paramNum] = safeStrings.get(param);
            aNotSafeTypes[paramNum++] = notSafeTypes.get(param);

            hasNoParamPrefix = hasNoParamPrefix || param.charAt(0) != SQLSession.paramPrefix;
        }
        assert !hasNoParamPrefix;

        // в общем случае неправильно использовать тот же механизм что и для параметров, но в текущей реализации будет работать
        paramArrays[paramNum] = BinaryWhere.adjustSelectivity.toCharArray();
//      params[paramNum] = ;
        aSafeStrings[paramNum] = new ParsedString(volatileStats && Settings.get().isEnableAdjustSelectivity() ? " OR " + syntax.getAdjustSelectivityPredicate() : "");
        aNotSafeTypes[paramNum++] = null;

        char[] toparse = statement.toCharArray();
        StringBuilder parsedString = new StringBuilder(envString);
        char[] parsed = new char[toparse.length + paramArrays.length * 100];
        int num = 0;
        for (int i = 0; i < toparse.length;) {
            int charParsed = 0;
            if(hasNoParamPrefix || toparse[i] == SQLSession.paramPrefix) { // optimization, later the same can be done with map and postfix
                for (int p = 0; p < paramArrays.length; p++) {
                    if (BaseUtils.startsWith(toparse, i, paramArrays[p])) { // нашли
                        String valueString;

                        Type notSafeType = aNotSafeTypes[p];
                        ParsedString safeString = aSafeStrings[p];
                        if (safeString != null) { // если можно вручную пропарсить парсим
                            valueString = safeString.getString();
                            safeString.fillEnv(mPreparedParams);
                        } else {
                            if (notSafeType instanceof ConcatenateType)
                                valueString = notSafeType.writeDeconc(syntax, ensureTypes);
                            else
                                valueString = "?";
                            mPreparedParams.add(aParams[p]);
                        }
                        if (notSafeType != null)
                            valueString = notSafeType.getCast(valueString, syntax, ensureTypes);

                        char[] valueArray = valueString.toCharArray();
                        if (num + valueArray.length >= parsed.length) {
                            parsedString.append(parsed, 0, num);
                            parsed = new char[BaseUtils.max(toparse.length - i + paramArrays.length * 100, valueArray.length + 100)];
                            num = 0;
                        }
                        System.arraycopy(valueArray, 0, parsed, num, valueArray.length);
                        num += valueArray.length;
                        charParsed = paramArrays[p].length;
                        assert charParsed != 0;
                        break;
                    }
                }
            }
            if (charParsed == 0) {
                if(num + 1 >= parsed.length) {
                    parsedString.append(parsed, 0, num);
                    parsed = new char[toparse.length - i + paramArrays.length * 100 + 1];
                    num = 0;
                }
                parsed[num++] = toparse[i];
                charParsed = 1;
            }
            i = i + charParsed;
        }
        parsedString.append(parsed, 0, num);

        return new ParsedParamString(parsedString.toString(), mPreparedParams.immutableList());
    }

    public ParsedStatement parseStatement(Connection connection, SQLSyntax syntax) throws SQLException {
        return getString(syntax).prepareStatement(connection);
    }

    public boolean calcTwins(TwinImmutableObject o) {
        return notSafeTypes.equals(((PreParsedStatement) o).notSafeTypes) && params.equals(((PreParsedStatement) o).params) && safeStrings.equals(((PreParsedStatement) o).safeStrings) && statement.equals(((PreParsedStatement) o).statement) && volatileStats == ((PreParsedStatement) o).volatileStats && envString.equals(((PreParsedStatement) o).envString);
    }

    public int immutableHashCode() {
        return 31 * (31 * (31 * (31 * (31 * statement.hashCode() + params.hashCode()) + safeStrings.hashCode()) + notSafeTypes.hashCode()) + ( volatileStats ? 1 : 0 )) + envString.hashCode();
    }
}
