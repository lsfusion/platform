package lsfusion.server.logics.event;

import lsfusion.server.logics.action.session.change.StructChanges;

public interface ApplyEvent {
    
    boolean hasChanges(StructChanges changes);
}
