package platform.server.data.translator;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.data.expr.*;

import java.util.HashMap;
import java.util.Map;

@Immutable
public class KeyTranslator extends Translator<KeyExpr> {

    public KeyTranslator(Map<KeyExpr, KeyExpr> iKeys, Map<ValueExpr, ValueExpr> iValues) {
        super(iKeys, iValues, true);
    }

    public <K> Map<K, BaseExpr> translateDirect(Map<K, ? extends BaseExpr> map) {
        Map<K, BaseExpr> transMap = new HashMap<K, BaseExpr>();
        for(Map.Entry<K,? extends BaseExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateDirect(this));
        return transMap;
    }

    // для кэша classWhere на самом деле надо
    public <K> Map<K, VariableClassExpr> translateVariable(Map<K, ? extends VariableClassExpr> map) {
        Map<K,VariableClassExpr> transMap = new HashMap<K, VariableClassExpr>();
        for(Map.Entry<K,? extends VariableClassExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateDirect(this));
        return transMap;
    }

    public <K> Map<K, Expr> translate(Map<K, ? extends Expr> map) {
        Map<K, Expr> transMap = new HashMap<K, Expr>();
        for(Map.Entry<K,? extends Expr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateDirect(this));
        return transMap;
    }

    public boolean identity() {
        return BaseUtils.identity(keys) && BaseUtils.identity(values);
    }
}
