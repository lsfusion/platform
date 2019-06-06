package lsfusion.server.physics.dev.id.name;

public class ShortDBNamingPolicy extends FixedSizeUnderscoreDBNamingPolicy {
    public ShortDBNamingPolicy(int maxIDLength) {
        super(maxIDLength, "_auto");
    }

    @Override
    public String transformActionOrPropertyCNToDBName(String canonicalName) {
        return cutToMaxLength(PropertyCanonicalNameParser.getName(canonicalName));
    }
    
    @Override
    public String transformTableCNToDBName(String canonicalName) {
        return cutToMaxLength(CanonicalNameUtils.getName(canonicalName));
    }
}