package lsfusion.server.logics;

import lsfusion.server.classes.sets.ResolveClassSet;

import java.util.List;

public class SimpleDBNamePolicy implements PropertyDBNamePolicy {
    private int MAX_LENGTH;

    public SimpleDBNamePolicy(int maxIDLength) {
        this.MAX_LENGTH = maxIDLength;
    }
    
    @Override
    public String createName(String namespaceName, String name, List<ResolveClassSet> signature) {
        String canonicalName = PropertyCanonicalNameUtils.createName(namespaceName, name, signature);
        return transformToDBName(canonicalName);
    }

    private String cutToMaxLength(String s) {
        if (s.length() > MAX_LENGTH) {
            s = s.substring(0, MAX_LENGTH);
        }
        return s;
    }
    
    @Override
    public String transformToDBName(String canonicalName) {
        int bracketPos = canonicalName.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
        if (bracketPos == -1) {
            bracketPos = canonicalName.length();
        }

        // отдельно обрабатываем канонические имена class data properties из-за того, что они получаются слишком длинными, а сигнатура в них необязательна для уникальности
        if (canonicalName.startsWith("System." + PropertyCanonicalNameUtils.classDataPropPrefix)) {
            return cutToMaxLength(canonicalName.substring(0, bracketPos).replaceAll("\\.", "_"));
        }

        assert bracketPos < canonicalName.length();

        //String signatureStr = canonicalName.substring(bracketPos);
        //signatureStr = signatureStr.replaceAll("[a-zA-Z0-9_]+\\.", "");

        int dotPos = canonicalName.indexOf(".");
        String dbName = canonicalName.substring(dotPos == -1 ? 0 : (dotPos + 1), bracketPos);// + signatureStr;
        dbName = dbName.replaceAll("\\?", "null");
        dbName = dbName.replaceAll("[^a-zA-Z0-9_]", "_");
        while (dbName.endsWith("_")) {
            dbName = dbName.substring(0, dbName.length() - 1); // убираем завершающие подчеркивания
        }
        return cutToMaxLength(dbName);
    }
    
}