package lsfusion.server.logics;

import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.form.entity.PropertyObjectEntity;

import java.util.List;

/**
 * Created by DAle on 20.03.14.
 * 
 */

public class DefaultSIDPolicy implements PropertySIDPolicy {
    @Override
    public String createSID(String namespaceName, String name, List<AndClassSet> signature, String oldName) {
        String canonicalName = PropertyCanonicalNameUtils.createName(namespaceName, name, signature);
        return transformCanonicalNameToSID(canonicalName);
    }

    @Override
    public String transformCanonicalNameToSID(String canonicalName) {
        int bracketPos = canonicalName.indexOf(PropertyCanonicalNameUtils.signatureLBracket);

        String signatureStr = canonicalName.substring(bracketPos);
        signatureStr = signatureStr.replaceAll("[a-zA-Z0-9_]+\\.", "");
        
        String sid = canonicalName.substring(0, bracketPos) + signatureStr;
        sid = sid.replaceAll("\\?", "null");
        sid = sid.replaceAll("[^a-zA-Z0-9_]", "_");
        return sid.substring(0, sid.length() - 1); // убираем завершающее подчеркивание 
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
        if (property.property.getName() == null) {  // Для обратной совместимости. Для OBJVALUE, SELECTION пока берется внутренний SID   
            return property.property.getSID();
        } else {
            return OldSIDPolicy.createPropertyDrawSID(property.property.getName(), property);
        }
    }
}
