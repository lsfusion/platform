package lsfusion.server.logics.mutables.impl;

import lsfusion.base.MutableObject;
import lsfusion.base.col.interfaces.mutable.MList;
import lsfusion.server.ServerLoggers;
import lsfusion.server.logics.mutables.Version;

import java.util.TreeMap;

public abstract class NFImpl<M, F> extends MutableObject {

    private Object changes;
    protected String getDebugInfo() {
        return null;
    }
    protected M getChanges() {
        if(checkFinal(changes)) {
            String debugInfo = getDebugInfo();
            throw new RuntimeException("NF COLLECTION RESTARTED" + (debugInfo !=null ? " " + debugInfo : ""));
//            ServerLoggers.assertLog(false, "NF COLLECTION RESTARTED" + (debugInfo !=null ? " " + debugInfo : ""));
//            changes = prevChanges;
        }
        return (M)changes;
    }

    private boolean allowVersionFinalRead;
    protected F proceedVersionFinal(Version version) {
        if(checkVersionFinal(version))
            return getFinalChanges();
        return null;
    }

    protected boolean checkVersionFinal(Version version) {
        return allowVersionFinalRead && version != Version.LAST && checkFinal(changes);
    }

    protected NFImpl() {
        this(false);
    }

    protected NFImpl(boolean allowVersionFinalRead) {
        changes = initMutable();
        this.allowVersionFinalRead = allowVersionFinalRead;
    }

    protected NFImpl(F changes) {
        this.changes = changes;
    }

    protected abstract M initMutable();
    public abstract F getNF(Version version);
    protected abstract boolean checkFinal(Object object);
    
//    Object prevChanges;
    protected F getFinal() {
        if(!checkFinal(changes)) {
            synchronized (this) {
                if(!checkFinal(changes)) {
//                    prevChanges = changes;
                    changes = getNF(Version.LAST);
                }
            }
        }
        return getFinalChanges();
    }
    
    protected F getFinalChanges() {
        return (F)changes; 
    }
    
    protected void setFinal(F set) {
        changes = set;
    }
}
