package lsfusion.server.session;

import org.apache.xpath.operations.Bool;

public class ChangeType {
    public static final ChangeType NOUPDATE = new ChangeType(true, null);

    private final boolean isFinal;
    private final Boolean setOrDropped;

    public ChangeType(boolean isFinal, Boolean setOrDropped) {
        this.isFinal = isFinal;
        this.setOrDropped = setOrDropped;
    }

    @Override
    public String toString() {
        return isFinal ? "FINAL" : "NOTFINAL"  + (setOrDropped == null ? "" : (setOrDropped ? " SET" : " DROPPED"));
    }

    private static final ChangeType[] types = new ChangeType[6];
    static {
        for(int i=0;i<2;i++)
            for(int j=0;j<3;j++) {
                boolean isFinal = (i==0);
                Boolean setOrDropped = (j==0 ? null : j==2);
                types[getIndex(isFinal, setOrDropped)] = new ChangeType(isFinal, setOrDropped);
            }
    }
    private static int getIndex(boolean isFinal, Boolean setOrDropped) {
        return (isFinal ? 3 : 0) + (setOrDropped == null ? 0 : (setOrDropped ? 2 : 1));
    }

    public static ChangeType get(boolean isFinal, Boolean setOrDropped) {
        return types[getIndex(isFinal, setOrDropped)];
    }

    public boolean isFinal() {
        return isFinal;
    }

    public Boolean getSetOrDropped() {
        return setOrDropped;
    }
}
