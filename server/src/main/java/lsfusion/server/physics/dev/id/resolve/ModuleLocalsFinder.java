package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.language.linear.LCP;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ModuleLocalsFinder extends ModulePropertyOrActionFinder<LCP<?>> {
    @Override
    protected Iterable<LCP<?>> getSourceList(LogicsModule module, String name) {
        return filterByName(name, module.getLocals());
    }

    private Iterable<LCP<?>> filterByName(String name, Map<LCP<?>, LogicsModule.LocalPropertyData> locals) {
        List<LCP<?>> res = new ArrayList<>();
        for (Map.Entry<LCP<?>, LogicsModule.LocalPropertyData> entry : locals.entrySet()) {
            if (entry.getValue().name.equals(name)) {
                res.add(entry.getKey());
            }
        }
        return res;
    }
}
