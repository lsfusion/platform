package lsfusion.server.physics.dev.id.name;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.sets.ResolveClassSet;

import java.util.List;

public class FixedSizeUnderscoreDBNamingPolicy implements DBNamingPolicy {
    private final int maxIDLength;
    private final String autoTablesPrefix; 

    public FixedSizeUnderscoreDBNamingPolicy(int maxIDLength, String autoTablesPrefix) {
        this.maxIDLength = maxIDLength;
        this.autoTablesPrefix = autoTablesPrefix;
    }

    @Override
    public String createPropertyName(String namespaceName, String name, List<ResolveClassSet> signature) {
        String canonicalName = PropertyCanonicalNameUtils.createName(namespaceName, name, signature);
        return transformPropertyCNToDBName(canonicalName);
    }

    @Override
    public String createAutoTableName(List<ValueClass> classes) {
        StringBuilder builder = new StringBuilder(autoTablesPrefix);
        for (ValueClass valueClass : classes) {
            builder.append('_');
            builder.append(valueClass.getSID());
        }
        return cutToMaxLength(builder.toString());
    }

    // Заменяет знаки '?' на 'null', затем все спец символы заменяет на '_', и удаляет подчеркивания в конце 
    @Override
    public String transformPropertyCNToDBName(String canonicalName) {
        String dbName = replaceUnknownClassesWithNull(canonicalName);
        dbName = replaceAllNonIDLettersWithUnderscore(dbName);
        dbName = removeTrailingUnderscores(dbName);
        return cutToMaxLength(dbName);
    }
 
    private String removeTrailingUnderscores(String name) {
        return name.replaceAll("_+$", "");
    }
    
    private String replaceAllNonIDLettersWithUnderscore(String name) {
        return name.replaceAll("[^a-zA-Z0-9_]", "_");
    }
    
    private String replaceUnknownClassesWithNull(String name) {
        return name.replace(PropertyCanonicalNameUtils.UNKNOWNCLASS, "null");
    }
    
    @Override
    public String transformTableCNToDBName(String canonicalName) {
        return cutToMaxLength(canonicalName.replace(CanonicalNameUtils.DELIMITER, '_'));
    }

    protected String cutToMaxLength(String s) {
        if (s.length() > maxIDLength) {
            s = s.substring(0, maxIDLength);
        }
        return s;
    }
}
