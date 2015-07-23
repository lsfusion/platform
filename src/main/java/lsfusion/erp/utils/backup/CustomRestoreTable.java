package lsfusion.erp.utils.backup;

import java.util.ArrayList;
import java.util.List;

public class CustomRestoreTable {
    List<String> sqlProperties;
    List<String> lcpProperties;
    List<String> keys;
    List<String> classKeys;

    public CustomRestoreTable() {
        this.sqlProperties = new ArrayList<>();
        this.lcpProperties = new ArrayList<>();
        this.keys = new ArrayList<>();
        this.classKeys = new ArrayList<>();
    }
}
