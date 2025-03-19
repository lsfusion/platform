package lsfusion.server.physics.dev.id.name;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.util.List;

public class FixedSizeUnderscoreDBNamingPolicy implements DBNamingPolicy {
    private final int maxIDLength;
    private final String autoTablesPrefix; 

    public FixedSizeUnderscoreDBNamingPolicy(int maxIDLength, String autoTablesPrefix) {
        this.maxIDLength = maxIDLength;
        this.autoTablesPrefix = autoTablesPrefix;
    }

    @Override
    public String createActionOrPropertyDBName(String namespaceName, String name, List<ResolveClassSet> signature) {
        String canonicalName = PropertyCanonicalNameUtils.createName(namespaceName, name, signature);
        return transformActionOrPropertyCNToDBName(canonicalName);
    }

    @Override
    public String createTableDBName(String namespace, String name) {
        String canonicalName = CanonicalNameUtils.createCanonicalName(namespace, name);
        return transformTableCNToDBName(canonicalName);
    }
    
    @Override
    public String createAutoTableDBName(List<ValueClass> classes) {
        StringBuilder builder = new StringBuilder(autoTablesPrefix);
        for (ValueClass valueClass : classes) {
            builder.append('_');
            builder.append(valueClass.getSID());
        }
        return cutToMaxLength(builder.toString());
    }

    // Заменяет знаки '?' на 'null', затем все спец символы заменяет на '_', и удаляет подчеркивания в конце 
    @Override
    public String transformActionOrPropertyCNToDBName(String canonicalName) {
        String dbName = replaceUnknownClassesWithNull(canonicalName);
        dbName = transformToIDSymbolsOnlyFormat(dbName);
        return cutToMaxLength(dbName);
    }

    protected String transformToIDSymbolsOnlyFormat(String name) {
        name = replaceAllNonIDLettersWithUnderscore(name);
        return removeTrailingUnderscores(name);
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
    
    @Override
    public String transformIndexNameToDBName(String indexName) {
        return cutToMaxLength(indexName);
    }
    
    protected String cutToMaxLength(String s) {
        if (s.length() > maxIDLength) {
            s = s.substring(0, maxIDLength);
        }
        return s;
    }
}
