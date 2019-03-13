package lsfusion.server.language.resolving;

import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LCP;

import java.util.List;

public class ModuleDirectLocalsFinder extends ModuleLocalsFinder {
    @Override
    protected boolean accepted(LogicsModule module, LCP<?> property, List<ResolveClassSet> signature) {
        return SignatureMatcher.isCompatible(module.getLocalSignature(property), signature, false, false);
    }
}
