package lsfusion.server.language.resolving;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.MetaCodeFragment;

public class ModuleMetaCodeFragmentFinder extends ModuleSingleElementFinder<MetaCodeFragment, Integer> {
    @Override
    protected MetaCodeFragment getElement(LogicsModule module, String simpleName, Integer paramCnt) {
        return module.getMetaCodeFragment(simpleName, paramCnt);
    }
}
