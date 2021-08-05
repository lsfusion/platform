package lsfusion.server.logics.action.session.change;

public class ChangeType {
    private final boolean isFinal;
    private final Boolean setOrDropped;

    public final boolean hasChanges;
    public final boolean hasPrevPrereads;

    public ChangeType(boolean isFinal, Boolean setOrDropped, boolean hasChanges, boolean hasPrevPrereads) {
        this.isFinal = isFinal;
        this.setOrDropped = setOrDropped;

        this.hasChanges = hasChanges;
        this.hasPrevPrereads = hasPrevPrereads;
    }

    @Override
    public String toString() {
        return (isFinal ? "FINAL " : " ")  +
                (setOrDropped == null ? "" : (setOrDropped ? "SET " : " DROPPED ")) +
                (hasChanges ? "HASCHANGES " : "") +
                (hasPrevPrereads ? "HASPREVPREREADS " : "");
    }

    private static final ChangeType[] types = new ChangeType[3 * 2 * 2 * 2];
    static {
        for(int i=0;i<2;i++)
            for(int k=0;k<2;k++)
                for(int l=0;l<2;l++)
                    for(int j=0;j<3;j++) {
                        boolean isFinal = (i==0);
                        Boolean setOrDropped = (j==0 ? null : j==2);
                        boolean hasChanges = (k==0);
                        boolean hasPrevPrereads = (l==0);
                        types[getIndex(isFinal, setOrDropped, hasChanges, hasPrevPrereads)] = new ChangeType(isFinal, setOrDropped, hasChanges, hasPrevPrereads);
                    }
    }
    private static int getIndex(boolean isFinal, Boolean setOrDropped, boolean hasChanges, boolean hasPrevPrereads) {
        return 2 * (2 * (2 * (setOrDropped == null ? 0 : (setOrDropped ? 2 : 1)) + (isFinal ? 1 : 0)) + (hasChanges ? 1 : 0)) + (hasPrevPrereads ? 1 : 0);
    }

    public static ChangeType get(boolean isFinal, Boolean setOrDropped, boolean hasChanges, boolean hasPrevPrereads) {
        return types[getIndex(isFinal, setOrDropped, hasChanges, hasPrevPrereads)];
    }

    public boolean isFinal() {
        return isFinal;
    }

    public Boolean getSetOrDropped() {
        return setOrDropped;
    }

    public ChangeType getPrev() {
        if(hasPrevPrereads)
            return get(false, null, false, true);
        return null;
    }
}
