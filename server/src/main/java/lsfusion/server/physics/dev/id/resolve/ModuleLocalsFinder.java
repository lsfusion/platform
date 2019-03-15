package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ModuleLocalsFinder extends ModulePropertyOrActionFinder<LP<?>> {
    @Override
    protected Iterable<LP<?>> getSourceList(LogicsModule module, String name) {
        return filterByName(name, module.getLocals());
    }

    private Iterable<LP<?>> filterByName(String name, Map<LP<?>, LogicsModule.LocalPropertyData> locals) {
        List<LP<?>> res = new ArrayList<>();
        for (Map.Entry<LP<?>, LogicsModule.LocalPropertyData> entry : locals.entrySet()) {
            if (entry.getValue().name.equals(name)) {
                res.add(entry.getKey());
            }
        }
        return res;
    }
}
