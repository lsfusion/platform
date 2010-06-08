package platform.server.view.form.filter;

import platform.interop.FilterType;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.Expr;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.session.DataSession;
import platform.server.view.form.*;
import platform.server.data.where.Where;
import platform.server.logics.DataObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public abstract class Filter implements Updated {

    // даже если не в интерфейсе все равно ставить (то есть по сути фильтр делать false)
    public final static boolean ignoreInInterface = true;

    public Filter() {
    }

    public Filter(DataInputStream inStream, RemoteForm form) throws IOException {
    }

    public static Filter deserialize(DataInputStream inStream, RemoteForm form) throws IOException, SQLException {
        byte type = inStream.readByte();
        switch(type) {
            case FilterType.OR:
                return new OrFilter(inStream, form);
            case FilterType.COMPARE:
                return new CompareFilter(inStream, form);
            case FilterType.NOTNULL:
                return new NotNullFilter(inStream, form);
            case FilterType.ISCLASS:
                return new IsClassFilter(inStream, form);
            case FilterType.NOT:
                return new NotFilter(inStream, form);
        }

        throw new IOException();
    }

    public abstract GroupObjectImplement getApplyObject();

    public abstract Where getWhere(Map<ObjectImplement, ? extends Expr> mapKeys, Set<GroupObjectImplement> classGroup, Modifier<? extends Changes> modifier) throws SQLException;

    public void resolveAdd(DataSession session, Modifier<? extends Changes> modifier, CustomObjectImplement object, DataObject addObject) throws SQLException {
    }
}
