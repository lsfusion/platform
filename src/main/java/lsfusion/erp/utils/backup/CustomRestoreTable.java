package lsfusion.erp.utils.backup;

import java.util.*;

public class CustomRestoreTable {
    Set<String> replaceOnlyNullSet;
    List<String> sqlProperties;
    List<String> lcpProperties;
    List<String> keys;
    List<String> classKeys;

    public CustomRestoreTable() {
        this.replaceOnlyNullSet = new HashSet<>();
        this.sqlProperties = new ArrayList<>();
        this.lcpProperties = new ArrayList<>();
        this.keys = new ArrayList<>();
        this.classKeys = new ArrayList<>();
    }
}
