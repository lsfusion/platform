package lsfusion.server.logics.form.interactive.design;

import lsfusion.server.physics.dev.debug.DebugInfo;

@FunctionalInterface
public interface ContainerFactory<C> {

    C createContainer(DebugInfo.DebugPoint debugPoint);
}
