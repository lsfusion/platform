package lsfusion.erp.utils.backup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CustomRestoreTable {
    Set<String> replaceOnlyNullSet;
    List<String> sqlProperties;
    List<String> lcpProperties;
    List<String> keys;
    List<String> classKeys;
    boolean restoreObjects;

    public CustomRestoreTable(boolean restoreObjects) {
        this.replaceOnlyNullSet = new HashSet<>();
        this.sqlProperties = new ArrayList<>();
        this.lcpProperties = new ArrayList<>();
        this.keys = new ArrayList<>();
        this.classKeys = new ArrayList<>();
        this.restoreObjects = restoreObjects;
    }
}
