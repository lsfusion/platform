package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;

import java.util.List;

public class ModuleDirectLocalsFinder extends ModuleLocalsFinder {
    @Override
    protected boolean accepted(LogicsModule module, LP<?> property, List<ResolveClassSet> signature) {
        return SignatureMatcher.isCompatible(module.getLocalSignature(property), signature, false, false);
    }
}
