package lsfusion.server.physics.dev.id.name;

public class ShortDBNamingPolicy extends FixedSizeUnderscoreDBNamingPolicy {
    public ShortDBNamingPolicy(int maxIDLength) {
        super(maxIDLength, "_auto");
    }

    @Override
    public String transformActionOrPropertyCNToDBName(String canonicalName) {
        String name = PropertyCanonicalNameParser.getName(canonicalName);
        return super.transformActionOrPropertyCNToDBName(name);
    }
    
    @Override
    public String transformTableCNToDBName(String canonicalName) {
        return cutToMaxLength(CanonicalNameUtils.getName(canonicalName));
    }
}