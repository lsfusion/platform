package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class ModuleLocalsFinder extends ModulePropertyOrActionFinder<LP<?>> {
    @Override
    protected Iterable<LP<?>> getSourceList(LogicsModule module, String name) {
        return filterByName(name, module.getLocals());
    }

    private Iterable<LP<?>> filterByName(String name, Map<LP<?>, LogicsModule.LocalPropertyData> locals) {
        // locals live only inside the list action being parsed, so for every module of the required closure but the one
        // currently parsed this map is empty - walking it used to cost a list and two iterators per module visit
        if (locals.isEmpty())
            return Collections.emptyList();

        List<LP<?>> res = null;
        for (Map.Entry<LP<?>, LogicsModule.LocalPropertyData> entry : locals.entrySet()) {
            if (entry.getValue().name.equals(name)) {
                if (res == null)
                    res = new ArrayList<>();
                res.add(entry.getKey());
            }
        }
        return res == null ? Collections.emptyList() : res;
    }
}
