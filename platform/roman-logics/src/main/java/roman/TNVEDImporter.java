package roman;

import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.col.interfaces.immutable.ImMap;
import platform.base.col.interfaces.immutable.ImOrderMap;
import platform.base.col.interfaces.immutable.ImRevMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.data.query.QueryBuilder;
import platform.server.form.instance.FormInstance;
import platform.server.logics.ObjectValue;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class TNVEDImporter {
    protected DataSession session;
    protected ObjectValue value;
    protected RomanLogicsModule LM;
    protected String classifierType = null;

    protected List<String> category10sids;

    public TNVEDImporter(FormInstance executeForm, ObjectValue value, RomanLogicsModule LM, String classifierType) {
        this(executeForm, value, LM);
        this.classifierType = classifierType;
    }

    public TNVEDImporter(FormInstance<?> form, ObjectValue value, RomanLogicsModule LM) {
        session = form.session;
        this.value = value;
        this.LM = LM;
    }

    public abstract void doImport() throws IOException, xBaseJException, SQLException;

    protected List<String> getFullCategory10() throws SQLException {
        List<String> list = new ArrayList<String>();
        ImRevMap<PropertyInterface, KeyExpr> keys = LM.sidCustomCategory10.getMapKeys();
        Expr expr = LM.sidCustomCategory10.property.getExpr(keys, session.getModifier());
        QueryBuilder<PropertyInterface, Object> query = new QueryBuilder<PropertyInterface, Object>(keys);
        query.addProperty("sid", expr);
        query.and(expr.getWhere());
        query.and(LM.customCategory4CustomCategory10.getExpr(session.getModifier(), keys.singleValue()).getWhere());
        ImOrderMap<ImMap<PropertyInterface, Object>, ImMap<Object, Object>> result = query.execute(session.sql);
        for (ImMap<PropertyInterface, Object> key : result.keyIt()) {
            list.add(result.get(key).get("sid").toString());
        }
        return list;
    }

    protected List<String> getCategory10(String code) throws SQLException {
        List<String> list = new ArrayList<String>();
        for (String sid10 : category10sids) {
            if (sid10.startsWith(code)) {
                list.add(sid10);
            }
        }
        return list;
    }
}
