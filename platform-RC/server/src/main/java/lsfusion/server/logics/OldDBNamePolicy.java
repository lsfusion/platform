package lsfusion.server.logics;

import lsfusion.server.classes.sets.ResolveClassSet;

import java.util.List;

/**
 * User: DAle
 * Date: 20.11.13
 * Time: 10:58
 */

public class OldDBNamePolicy implements PropertyDBNamePolicy {
    @Override
    public String createName(String namespaceName, String name, List<ResolveClassSet> signature) {
        if (namespaceName == null) {
            return name;
        } else {
            return namespaceName + "_" + name;
        }
    }

    @Override
    public String transformToDBName(String canonicalName) {
        String sid = canonicalName.replace(".", "_");
        int bracketPos = sid.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
        if (bracketPos >= 0) {
            sid = sid.substring(0, bracketPos);
        }
        return sid;
    }
}
