package platform.server.form.instance.filter;

import platform.interop.FilterType;
import platform.server.data.expr.Expr;
import platform.server.data.where.Where;
import platform.server.form.instance.*;
import platform.server.logics.DataObject;
import platform.server.logics.NullValue;
import platform.server.session.Changes;
import platform.server.session.DataSession;
import platform.server.session.Modifier;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

public abstract class FilterInstance implements Updated {

    // даже если не в интерфейсе все равно ставить (то есть по сути фильтр делать false)
    public final static boolean ignoreInInterface = true;

    public FilterInstance() {
    }

    public FilterInstance(DataInputStream inStream, FormInstance form) throws IOException {
    }

    public static FilterInstance deserialize(DataInputStream inStream, FormInstance form) throws IOException, SQLException {
        byte type = inStream.readByte();
        switch(type) {
            case FilterType.OR:
                return new OrFilterInstance(inStream, form);
            case FilterType.COMPARE:
                CompareFilterInstance filter = new CompareFilterInstance(inStream, form);
                if (filter.value instanceof NullValue)
                    return new NotFilterInstance(new NotNullFilterInstance(filter.property));
                else
                    return filter;
            case FilterType.NOTNULL:
                return new NotNullFilterInstance(inStream, form);
            case FilterType.ISCLASS:
                return new IsClassFilterInstance(inStream, form);
            case FilterType.NOT:
                return new NotFilterInstance(inStream, form);
        }

        throw new IOException();
    }

    public abstract GroupObjectInstance getApplyObject();

    public abstract Where getWhere(Map<ObjectInstance, ? extends Expr> mapKeys, Modifier<? extends Changes> modifier);

    public void resolveAdd(DataSession session, Modifier<? extends Changes> modifier, CustomObjectInstance object, DataObject addObject) throws SQLException {
    }
}
