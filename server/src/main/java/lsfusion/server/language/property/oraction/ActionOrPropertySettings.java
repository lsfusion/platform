package lsfusion.server.language.property.oraction;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;

public class ActionOrPropertySettings {
    public String groupName = null;
    public ImSet<String> annotations = SetFact.EMPTY();

    public void addAnnotation(String annotation) {
        annotations = annotations.merge(annotation);
    }
}
