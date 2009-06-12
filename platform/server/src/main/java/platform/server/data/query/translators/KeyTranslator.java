package platform.server.data.query.translators;

import platform.server.data.query.Context;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;

import java.util.Map;

public class KeyTranslator extends DirectJoinTranslator {

    public KeyTranslator(Context transContext,Map<KeyExpr, KeyExpr> iKeys, Map<ValueExpr, ValueExpr> iValues) {
        super(iKeys, iValues);
        context = new Context(transContext,this);
    }
}
