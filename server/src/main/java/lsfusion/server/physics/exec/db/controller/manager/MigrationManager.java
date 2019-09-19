package lsfusion.server.physics.exec.db.controller.manager;

import lsfusion.base.col.heavy.OrderedMap;
import lsfusion.server.physics.dev.id.name.DBNamingPolicy;

import java.util.*;

public class MigrationManager {
    private static Comparator<MigrationVersion> migrationVersionComparator = MigrationVersion::compare;

    private TreeMap<MigrationVersion, List<SIDChange>> propertyCNChanges = new TreeMap<>(migrationVersionComparator);
    private TreeMap<MigrationVersion, List<SIDChange>> actionCNChanges = new TreeMap<>(migrationVersionComparator);
    private TreeMap<MigrationVersion, List<SIDChange>> propertyDrawNameChanges = new TreeMap<>(migrationVersionComparator);
    private TreeMap<MigrationVersion, List<SIDChange>> storedPropertyCNChanges = new TreeMap<>(migrationVersionComparator);
    private TreeMap<MigrationVersion, List<SIDChange>> classSIDChanges = new TreeMap<>(migrationVersionComparator);
    private TreeMap<MigrationVersion, List<SIDChange>> tableSIDChanges = new TreeMap<>(migrationVersionComparator);
    private TreeMap<MigrationVersion, List<SIDChange>> objectSIDChanges = new TreeMap<>(migrationVersionComparator);
    private TreeMap<MigrationVersion, List<SIDChange>> navigatorCNChanges = new TreeMap<>(migrationVersionComparator);

    private DBNamingPolicy policy;
    
    public MigrationManager(DBNamingPolicy policy) {
        this.policy = policy;
    }
    
    public Map<String, String> getPropertyCNChangesAfter(MigrationVersion versionAfter) {
        return getChangesAfter(versionAfter, propertyCNChanges);
    }

    public Map<String, String> getActionCNChangesAfter(MigrationVersion versionAfter) {
        return getChangesAfter(versionAfter, actionCNChanges);
    }

    public Map<String, String> getPropertyDrawNameChangesAfter(MigrationVersion versionAfter) {
        return getChangesAfter(versionAfter, propertyDrawNameChanges);
    }

    public Map<String, String> getStoredPropertyCNChangesAfter(MigrationVersion versionAfter) {
        return getChangesAfter(versionAfter, storedPropertyCNChanges);
    }

    public Map<String, String> getClassSIDChangesAfter(MigrationVersion versionAfter) {
        return getChangesAfter(versionAfter, classSIDChanges);
    }

    public Map<String, String> getTableSIDChangesAfter(MigrationVersion versionAfter) {
        return getChangesAfter(versionAfter, tableSIDChanges);
    }

    public Map<String, String> getObjectSIDChangesAfter(MigrationVersion versionAfter) {
        return getChangesAfter(versionAfter, objectSIDChanges);
    }

    public Map<String, String> getNavigatorCNChangesAfter(MigrationVersion versionAfter) {
        return getChangesAfter(versionAfter, navigatorCNChanges);
    }
    
    private Map<String, String> getChangesAfter(MigrationVersion versionAfter, TreeMap<MigrationVersion, List<SIDChange>> allChanges) {
        Map<String, String> resultChanges = new OrderedMap<>();

        for (Map.Entry<MigrationVersion, List<SIDChange>> changesEntry : allChanges.entrySet()) {
            if (changesEntry.getKey().compare(versionAfter) > 0) {
                List<SIDChange> versionChanges = changesEntry.getValue();
                Map<String, String> versionChangesMap = new OrderedMap<>();

                for (SIDChange change : versionChanges) {
                    if (versionChangesMap.containsKey(change.oldSID)) {
                        throw new RuntimeException(String.format("Renaming '%s' twice in version %s.", change.oldSID, changesEntry.getKey()));
                    }
                    versionChangesMap.put(change.oldSID, change.newSID);
                }

                // Если в текущей версии есть переименование a -> b, а в предыдущих версиях есть c -> a, то заменяем c -> a на c -> b
                for (Map.Entry<String, String> currentChanges : resultChanges.entrySet()) {
                    String renameTo = currentChanges.getValue();
                    if (versionChangesMap.containsKey(renameTo)) {
                        currentChanges.setValue(versionChangesMap.get(renameTo));
                        versionChangesMap.remove(renameTo);
                    }
                }

                // Добавляем оставшиеся (которые не получилось добавить к старым цепочкам) переименования из текущей версии в общий результат
                for (Map.Entry<String, String> change : versionChangesMap.entrySet()) {
                    if (resultChanges.containsKey(change.getKey())) {
                        throw new RuntimeException(String.format("Renaming '%s' twice", change.getKey()));
                    }
                    resultChanges.put(change.getKey(), change.getValue());
                }

                // Проверяем, чтобы не было нескольких переименований в одно и то же
                Set<String> renameToSIDs = new HashSet<>();
                for (String renameTo : resultChanges.values()) {
                    if (renameToSIDs.contains(renameTo)) {
                        throw new RuntimeException(String.format("Renaming to '%s' twice.", renameTo));
                    }
                    renameToSIDs.add(renameTo);
                }
            }
        }
        return resultChanges;
    }

    private MigrationVersion maxMigrationVersion = null;

    public void addMigrationVersion(String version) {
        MigrationVersion migrationVersion = new MigrationVersion(version);
        if (maxMigrationVersion == null || maxMigrationVersion.compare(migrationVersion) < 0) {
            maxMigrationVersion = migrationVersion;
        }
    }

    public void checkMigrationVersion(MigrationVersion oldMigrationVersion) {
        if (maxMigrationVersion != null && maxMigrationVersion.compare(oldMigrationVersion) < 0) {
            throw new RuntimeException(String.format("version of migration script (%s) is less than version of database (%s).",
                    maxMigrationVersion.toString(),
                    oldMigrationVersion.toString())
            );
        }
    }

    public MigrationVersion getCurrentMigrationVersion(MigrationVersion oldVersion) {
        if (maxMigrationVersion != null && maxMigrationVersion.compare(oldVersion) > 0) {
            return maxMigrationVersion;
        }
        return oldVersion;
    }

    public void addPropertyCNChange(String version, String oldName, String oldSignature, String newName, String newSignature, boolean stored) {
        if (newSignature == null) {
            newSignature = oldSignature;
        }
        addSIDChange(propertyCNChanges, version, oldName + oldSignature, newName + newSignature);
        if (stored) {
            addStoredPropertyCNChange(version, oldName + oldSignature, newName + newSignature);
        }
    }

    public void addStoredPropertyCNChange(String version, String oldCN, String newCN) {
        addSIDChange(storedPropertyCNChanges, version, oldCN, newCN);
    }
    
    public void addActionCNChange(String version, String oldName, String oldSignature, String newName, String newSignature) {
        if (newSignature == null) {
            newSignature = oldSignature;
        }
        addSIDChange(actionCNChanges, version, oldName + oldSignature, newName + newSignature);
    }

    public void addClassSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(classSIDChanges, version, transformUSID(oldSID), transformUSID(newSID));
    }

    public void addTableSIDChange(String version, String oldCN, String newCN) {
        addSIDChange(tableSIDChanges, version, policy.transformTableCNToDBName(oldCN),
                policy.transformTableCNToDBName(newCN));
    }

    public void addObjectSIDChange(String version, String oldSID, String newSID) {
        addSIDChange(objectSIDChanges, version, transformObjectUSID(oldSID), transformObjectUSID(newSID));
    }

    public void addPropertyDrawSIDChange(String version, String oldName, String newName) {
        addSIDChange(propertyDrawNameChanges, version, oldName, newName);
    }

    public void addNavigatorElementCNChange(String version, String oldCN, String newCN) {
        addSIDChange(navigatorCNChanges, version, oldCN, newCN);
    }

    private void addSIDChange(TreeMap<MigrationVersion, List<SIDChange>> sidChanges, String version, String oldSID, String newSID) {
        MigrationVersion migrationVersion = new MigrationVersion(version);
        sidChanges.putIfAbsent(migrationVersion, new ArrayList<>());
        sidChanges.get(migrationVersion).add(new SIDChange(oldSID, newSID));
    }

    private String transformUSID(String userSID) {
        return userSID.replaceFirst("\\.", "_");
    }

    private String transformObjectUSID(String userSID) {
        if (userSID.indexOf(".") != userSID.lastIndexOf(".")) {
            return transformUSID(userSID);
        }
        return userSID;
    }

    private static class SIDChange {
        public String oldSID;
        public String newSID;

        public SIDChange(String oldSID, String newSID) {
            this.oldSID = oldSID;
            this.newSID = newSID;
        }
    }
}
