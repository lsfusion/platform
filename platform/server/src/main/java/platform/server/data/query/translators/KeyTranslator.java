package platform.server.data.query.translators;

import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.VariableClassExpr;

import java.util.HashMap;
import java.util.Map;

import net.jcip.annotations.Immutable;

@Immutable
public class KeyTranslator extends Translator<KeyExpr> {

    public KeyTranslator(Map<KeyExpr, KeyExpr> iKeys, Map<ValueExpr, ValueExpr> iValues) {
        super(iKeys, iValues);
    }

    public <K> Map<K, AndExpr> translateDirect(Map<K, ? extends AndExpr> map) {
        Map<K,AndExpr> transMap = new HashMap<K, AndExpr>();
        for(Map.Entry<K,? extends AndExpr> entry : map.entrySet())
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

}
