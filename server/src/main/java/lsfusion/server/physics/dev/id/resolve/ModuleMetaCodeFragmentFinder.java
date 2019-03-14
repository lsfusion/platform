package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.MetaCodeFragment;
import lsfusion.server.logics.LogicsModule;

public class ModuleMetaCodeFragmentFinder extends ModuleSingleElementFinder<MetaCodeFragment, Integer> {
    @Override
    protected MetaCodeFragment getElement(LogicsModule module, String simpleName, Integer paramCnt) {
        return module.getMetaCodeFragment(simpleName, paramCnt);
    }
}
