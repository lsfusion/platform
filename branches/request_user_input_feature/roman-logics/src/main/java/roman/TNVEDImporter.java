package roman;

import org.xBaseJ.xBaseJException;
import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.query.Query;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.ObjectValue;
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
        Map<Object, KeyExpr> keys = LM.sidCustomCategory10.getMapKeys();
        Expr expr = LM.sidCustomCategory10.property.getExpr(keys, session.modifier);
        Query<Object, Object> query = new Query<Object, Object>(keys);
        query.properties.put("sid", expr);
        query.and(expr.getWhere());
        query.and(LM.customCategory4CustomCategory10.getExpr(session.modifier, BaseUtils.singleValue(keys)).getWhere());
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);
        for (Map<Object, Object> key : result.keySet()) {
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
