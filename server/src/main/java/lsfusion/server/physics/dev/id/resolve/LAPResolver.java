package lsfusion.server.physics.dev.id.resolve;

import lsfusion.base.BaseUtils;
import lsfusion.server.language.linear.LAP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.physics.dev.id.resolve.NamespaceElementFinder.FoundItem;

import java.util.ArrayList;
import java.util.List;

public class LAPResolver<L extends LAP<?, ?>> extends ElementResolver<L, List<ResolveClassSet>> {
    private final boolean filter;
    private final boolean prioritizeNotEquals;

    public LAPResolver(LogicsModule startModule, ModuleFinder<L, List<ResolveClassSet>> finder, boolean filter, boolean prioritizeNotEquals) {
        super(startModule, finder);
        this.filter = filter;
        this.prioritizeNotEquals = prioritizeNotEquals;
    }

    @Override
    protected List<FoundItem<L>> finalizeNamespaceResult(List<FoundItem<L>> result, String name, List<ResolveClassSet> param) {
        return result;
    }

    @Override
    protected FoundItem<L> finalizeResult(List<FoundItem<L>> result, String name, List<ResolveClassSet> param) throws ResolvingErrors.ResolvingError {
        FoundItem<L> finalItem = new FoundItem<>(null, null);
        if (!result.isEmpty()) {
            if (filter) {
                if (prioritizeNotEquals) {
                    result = prioritizeNotEquals(result, param);
                }
                result = NamespaceLAPFinder.filterFoundProperties(result);
            }
            if (result.size() > 1) {
                throw new ResolvingErrors.ResolvingAmbiguousPropertyError(result, name);
            } else if (result.size() == 1) {
                finalItem = result.get(0);
            }
        }
        return finalItem;
    }

    private List<FoundItem<L>> prioritizeNotEquals(List<FoundItem<L>> result, List<ResolveClassSet> param) {
        assert !result.isEmpty();
        List<FoundItem<L>> equals = new ArrayList<>();
        List<FoundItem<L>> notEquals = new ArrayList<>();
        for (FoundItem<L> item : result) {
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
