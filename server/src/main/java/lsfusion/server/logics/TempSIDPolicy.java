package lsfusion.server.logics;

import lsfusion.base.BaseUtils;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.form.entity.PropertyObjectEntity;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: DAle
 * Date: 20.11.13
 * Time: 9:06
 */

public class TempSIDPolicy implements PropertySIDPolicy {
    @Override
    public String createSID(String namespaceName, String name, List<AndClassSet> signature, String oldName) {
        String actualName = oldName == null ? name : oldName;
        if (namespaceName == null) {
            return actualName;
        } else {
            return namespaceName + "_" + actualName;
        }
    }

    @Override
    public String transformCanonicalNameToSID(String canonicalName) {
        String sid = canonicalName.replace(".", "_");
        int bracketPos = sid.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
        if (bracketPos >= 0) {
            sid = sid.substring(0, bracketPos);
        }
        return sid;
    }

    @Override
    public String createPropertyDrawSID(PropertyObjectEntity<?, ?> property) {
        if (property.property.getName() == null) {  // Для обратной совместимости. Для OBJVALUE, SELECTION пока берется внутренний SID   
            return property.property.getSID();
        } else {
            return OldSIDPolicy.createPropertyDrawSID(BaseUtils.nvl(property.property.getOldName(), property.property.getName()), property);
        }
    }
}
