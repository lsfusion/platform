package platform.server.data.query.translators;

import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.JoinExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.wheres.JoinWhere;

import java.util.Map;

// direct транслятор сохраняющий classWhere
public interface DirectTranslator extends Translator<KeyExpr, JoinExpr, JoinWhere> {

    <K> Map<K,AndExpr> translateAnd(Map<K,AndExpr> map);
}
