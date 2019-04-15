package lsfusion.server.physics.dev.id.name;

public class NamespaceDBNamingPolicy extends FixedSizeUnderscoreDBNamingPolicy {
    public NamespaceDBNamingPolicy(int maxIDLength) {
        super(maxIDLength, "_auto");
    }

    @Override
    public String transformActionOrPropertyCNToDBName(String canonicalName) {
        String namespace = PropertyCanonicalNameParser.getNamespace(canonicalName);
        String name = PropertyCanonicalNameParser.getName(canonicalName);
        String compoundName = CompoundNameUtils.createCompoundName(namespace, name);
        return super.transformActionOrPropertyCNToDBName(compoundName);
    }
}