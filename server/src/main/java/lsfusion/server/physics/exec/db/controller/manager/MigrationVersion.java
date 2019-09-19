package lsfusion.server.physics.exec.db.controller.manager;

import java.util.ArrayList;
import java.util.List;

public class MigrationVersion {
    private List<Integer> version;

    public MigrationVersion(String version) {
        this.version = versionToList(version);
    }

    public static List<Integer> versionToList(String version) {
        String[] splitArr = version.split("\\.");
        List<Integer> intVersion = new ArrayList<>();
        for (String part : splitArr) {
            intVersion.add(Integer.parseInt(part));
        }
        return intVersion;
    }

    public int compare(MigrationVersion rhs) {
        return compareVersions(version, rhs.version);
    }

    public static int compareVersions(List<Integer> lhs, List<Integer> rhs) {
        for (int i = 0; i < Math.max(lhs.size(), rhs.size()); i++) {
            int left = (i < lhs.size() ? lhs.get(i) : 0);
            int right = (i < rhs.size() ? rhs.get(i) : 0);
            if (left < right) return -1;
            if (left > right) return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < version.size(); i++) {
            if (i > 0) {
                buf.append(".");
            }
            buf.append(version.get(i));
        }
        return buf.toString();
    }
}

