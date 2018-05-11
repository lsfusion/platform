package lsfusion.server.logics;

public class DefaultDBNamingPolicy extends FixedSizeUnderscoreDBNamingPolicy {
    public DefaultDBNamingPolicy(int maxIDLength) {
        super(maxIDLength, "_auto");
    }
    
    @Override
    public String transformPropertyCNToDBName(String canonicalName) {
        int signtaturePos = canonicalName.indexOf(PropertyCanonicalNameUtils.signatureLBracket);
        assert signtaturePos > 0;
        
        // отдельно обрабатываем канонические имена class data properties из-за того, что они получаются слишком длинными, 
        // а сигнатура в них необязательна для уникальности
        if (canonicalName.startsWith("System." + PropertyCanonicalNameUtils.classDataPropPrefix)) {
            return cutToMaxLength(nameWithoutSignature(canonicalName, signtaturePos).replace(CompoundNameUtils.DELIMITER, '_'));
        }
        
        String signatureStr = canonicalName.substring(signtaturePos);
        signatureStr = removeNamespacesFromSignatureClasses(signatureStr);
        
        return super.transformPropertyCNToDBName(canonicalName.substring(0, signtaturePos) + signatureStr);
    }
    
    private String removeNamespacesFromSignatureClasses(String signatureStr) {
        return signatureStr.replaceAll("[a-zA-Z0-9_]+\\.", "");
    } 
    
    private String nameWithoutSignature(String name, int signaturePos) {
        return name.substring(0, signaturePos);
    }
}
