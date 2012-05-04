package platform.server.data.expr.query;

import platform.server.caches.ManualLazy;

public enum KeyStat {
    INFINITE, MANY, FEW;

    public boolean less(KeyStat stat) {
        return (equals(FEW) && !stat.equals(FEW)) || (equals(MANY) && stat.equals(INFINITE));
    }

    public KeyStat max(KeyStat stat) {
        return less(stat)?stat:this;
    }

    public boolean isMin() {
        return equals(FEW);
    }
}
