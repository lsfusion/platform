package platform.server.data.query.translators;

import net.jcip.annotations.Immutable;
import platform.base.BaseUtils;
import platform.server.data.query.exprs.*;

import java.util.HashMap;
import java.util.Map;

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

    public <K> Map<K, SourceExpr> translate(Map<K, ? extends SourceExpr> map) {
        Map<K,SourceExpr> transMap = new HashMap<K,SourceExpr>();
        for(Map.Entry<K,? extends SourceExpr> entry : map.entrySet())
            transMap.put(entry.getKey(),entry.getValue().translateDirect(this));
        return transMap;
    }

    public boolean identity() {
        return BaseUtils.identity(keys) && BaseUtils.identity(values);
    }
}
