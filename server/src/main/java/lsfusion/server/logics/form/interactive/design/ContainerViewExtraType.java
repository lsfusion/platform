package lsfusion.server.logics.form.interactive.design;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.interop.form.property.PropertyReadType;

public enum ContainerViewExtraType {
    CAPTION {
        @Override
        public byte getContainerReadType() {
            return PropertyReadType.CONTAINER_CAPTION;
        }
    };

    public static final ImSet<ContainerViewExtraType> extras = SetFact.toSet(values());

    public byte getContainerReadType() {
        throw new UnsupportedOperationException();
    }
}
