package lsfusion.server.logics.resolving;

import lsfusion.base.BaseUtils;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.resolving.NamespaceElementFinder.FoundItem;

import java.util.ArrayList;
import java.util.List;

public class LPResolver extends ElementResolver<LP<?, ?>, List<ResolveClassSet>> {
    private final boolean filter;
    private final boolean prioritizeNotEquals;

    public LPResolver(LogicsModule LM, ModuleFinder<LP<?, ?>, List<ResolveClassSet>> finder, boolean filter, boolean prioritizeNotEquals) {
        super(LM, finder);
        this.filter = filter;
        this.prioritizeNotEquals = prioritizeNotEquals;
    }

    @Override
    protected List<FoundItem<LP<?, ?>>> finalizeNamespaceResult(List<FoundItem<LP<?, ?>>> result, String name, List<ResolveClassSet> param) {
        return result;
    }

    @Override
    protected FoundItem<LP<?, ?>> finalizeResult(List<FoundItem<LP<?, ?>>> result, String name, List<ResolveClassSet> param) throws ResolvingErrors.ResolvingError {
        FoundItem<LP<?, ?>> finalItem = new FoundItem<>(null, null);
        if (!result.isEmpty()) {
            if (filter) {
                if (prioritizeNotEquals) {
                    result = prioritizeNotEquals(result, param);
                }
                result = NamespaceLPFinder.filterFoundProperties(result);
            }
            if (result.size() > 1) {
                throw new ResolvingErrors.ResolvingAmbiguousPropertyError(result, name);
            } else if (result.size() == 1) {
                finalItem = result.get(0);
            }
        }
        return finalItem;
    }

    private List<FoundItem<LP<?, ?>>> prioritizeNotEquals(List<FoundItem<LP<?, ?>>> result, List<ResolveClassSet> param) {
        assert !result.isEmpty();
        List<FoundItem<LP<?, ?>>> equals = new ArrayList<>();
        List<FoundItem<LP<?, ?>>> notEquals = new ArrayList<>();
        for (FoundItem<LP<?, ?>> item : result) {
            if (!BaseUtils.nullHashEquals(item.module.getParamClasses(item.value), param))
                notEquals.add(item);
            else
                equals.add(item);
        }

        if(!notEquals.isEmpty())
            return notEquals;
        else
            return equals;
    }
}
