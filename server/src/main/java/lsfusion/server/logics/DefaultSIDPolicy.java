package lsfusion.server.logics;

import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.form.entity.PropertyObjectEntity;

import java.util.List;

/**
 * Created by DAle on 20.03.14.
 * 
 */

public class DefaultSIDPolicy implements PropertySIDPolicy {
    private int MAX_LENGTH;
    
    public DefaultSIDPolicy(int maxIDLength) {
        this.MAX_LENGTH = maxIDLength;
    }
    
    @Override
    public String createSID(String namespaceName, String name, List<AndClassSet> signature) {
        String canonicalName = PropertyCanonicalNameUtils.createName(namespaceName, name, signature);
        return transformCanonicalNameToSID(canonicalName);
    }

    private String cutToMaxLength(String sid) {
        if (sid.length() > MAX_LENGTH) {
            sid = sid.substring(0, MAX_LENGTH);
        }
        return sid;
    }
    
    @Override
    public String transformCanonicalNameToSID(String canonicalName) {
        int bracketPos = canonicalName.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
        if (bracketPos == -1) {
            bracketPos = canonicalName.length();
        }
        
        // отдельно обрабатываем канонические имена class data properties из-за того, что они получаются слишком длинными, а сигнатура в них необязательна для уникальности
        if (canonicalName.startsWith("System." + PropertyCanonicalNameUtils.classDataPropPrefix)) {
            return cutToMaxLength(canonicalName.substring(0, bracketPos).replaceAll("\\.", "_"));
        }
        
        assert bracketPos < canonicalName.length();

        String signatureStr = canonicalName.substring(bracketPos);
        signatureStr = signatureStr.replaceAll("[a-zA-Z0-9_]+\\.", "");
        
        String sid = canonicalName.substring(0, bracketPos) + signatureStr;
        sid = sid.replaceAll("\\?", "null");
        sid = sid.replaceAll("[^a-zA-Z0-9_]", "_");
        while (sid.endsWith("_")) {
            sid = sid.substring(0, sid.length() - 1); // убираем завершающие подчеркивания
        }
        return cutToMaxLength(sid);
    }
    
    // todo [dale]: temporary
    public static String staticTransformCanonicalNameToSID(String canonicalName) {
        int bracketPos = canonicalName.indexOf(PropertyCanonicalNameUtils.signatureLBracket);

        String signatureStr = canonicalName.substring(bracketPos);
        signatureStr = signatureStr.replaceAll("[a-zA-Z0-9_]+\\.", "");

        String sid = canonicalName.substring(0, bracketPos) + signatureStr;
        sid = sid.replaceAll("\\?", "null");
        sid = sid.replaceAll("[^a-zA-Z0-9_]", "_");
        return sid.substring(0, sid.length() - 1); // убираем завершающее подчеркивание 
    }
    
    @Override
    public String createPropertyDrawSID(PropertyObjectEntity<?, ?> property) {
        assert property.property.isNamed();
        return OldSIDPolicy.createPropertyDrawSID(property.property.getName(), property);
    }
}
