package lsfusion.server.base.version.impl;

import lsfusion.base.mutability.MutableObject;
import lsfusion.server.base.version.Version;

import java.util.function.Supplier;

public abstract class NFImpl<M, F> extends MutableObject implements NF {

    // volatile : addChange publishes the change map late (it is created on the first write), and both the finalization check below
    // and the last-version read paths look at this field without holding the monitor
    private volatile Object changes;
    protected String getDebugInfo() {
        return null;
    }
    protected M getChanges() {
        Object changes = this.changes; // one look : the finalization can swap the field between two
        if(checkFinal(changes)) {
            String debugInfo = getDebugInfo();
            throw new RuntimeException("NF COLLECTION RESTARTED" + (debugInfo !=null ? " " + debugInfo : ""));
//            ServerLoggers.assertLog(false, "NF COLLECTION RESTARTED" + (debugInfo !=null ? " " + debugInfo : ""));
//            changes = prevChanges;
        }
        return (M)changes;
    }
    // what a read looks at : as getChanges, but the restart it meets is a read's own, which a copy's read is made again
    // on (readCopy)
    protected M readChanges() {
        Object changes = this.changes;
        if(checkFinal(changes)) {
            String debugInfo = getDebugInfo();
            throw new RestartedException("NF COLLECTION RESTARTED" + (debugInfo !=null ? " " + debugInfo : ""));
        }
        return (M)changes;
    }
    protected Object getChangesAsIs() {
        return changes;
    }

    // the restart a read's look at its changes meets, before the read runs any of them : one that leaves a change is
    // a plain failure again (NFChangeImpl.proceed)
    protected static class RestartedException extends RuntimeException {
        public RestartedException(String message) {
            super(message);
        }
    }

    // a copy - a form built from another one (FORMS) keeps copies of the other's collections - reads its source as it
    // is finalized itself, and the start finalizes forms in parallel chunks, so the source can be finalized by its own
    // chunk between the read's check for being final and its next look at the changes. Such a read is made again : it
    // then finds the final value - the one the finalization made of the very changes it was reading. Only on a restart
    // its own look meets, before it runs any change, so nothing is lost : a write's, or one met inside a change - a read
    // the change makes, of another collection or of this one -, fails as before. And without a lock : a copy's read
    // runs the mapping of the forms it goes through, which waits for their form, and a form is finalized under its own
    // monitor (see FormEntity.finalizeAroundInit)
    protected <R> R readCopy(Supplier<R> read) {
        try {
            return read.get();
        } catch (RestartedException e) {
            return read.get();
        }
    }

    public boolean isFinal() {
        return checkFinal(changes);
    }
    protected void setChanges(M changes) {
        this.changes = changes;
    }

    protected F proceedVersionFinal(Version version, boolean allowRead) {
        if(checkVersionFinal(version, allowRead))
            return getFinalChanges();
        return null;
    }

    protected boolean checkVersionFinal(Version version, boolean allowRead) {
        return allowRead && checkFinal(changes);
    }

    protected NFImpl() { // no accumulator until something is actually written - most cells never are
    }

    protected NFImpl(F changes) {
        this.changes = changes;
    }

    public abstract F getNF(Version version);
    protected abstract boolean checkFinal(Object object);
    
//    Object prevChanges;
    protected F getFinal() {
        if(!checkFinal(changes)) {
            synchronized (this) {
                if(!checkFinal(changes)) {
//                    prevChanges = changes;
                    changes = getNF(Version.last());
                }
            }
        }
        return getFinalChanges();
    }
    
    protected F getFinalChanges() {
        return (F)changes; 
    }
    
    public void finalizeChanges() {
        getFinal();
    }
}
