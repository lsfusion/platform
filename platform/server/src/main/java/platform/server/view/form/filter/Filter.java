package platform.server.view.form.filter;

import platform.interop.FilterType;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.session.TableChanges;
import platform.server.session.TableModifier;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.form.ObjectImplement;
import platform.server.view.form.RemoteForm;
import platform.server.view.form.Updated;
import platform.server.where.Where;

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

    public abstract Where getWhere(Map<ObjectImplement, KeyExpr> mapKeys, Set<GroupObjectImplement> classGroup, TableModifier<? extends TableChanges> modifier) throws SQLException;
}
