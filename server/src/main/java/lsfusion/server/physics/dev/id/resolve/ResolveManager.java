package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.metacode.MetaCodeFragment;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.sets.ResolveClassSet;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.group.AbstractGroup;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.logics.navigator.window.AbstractWindow;
import lsfusion.server.physics.dev.id.name.CompoundNameUtils;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.util.List;

public class ResolveManager {
    public LogicsModule LM;
    
    private ElementResolver<LP<?>, List<ResolveClassSet>> directLCPResolver;
    private ElementResolver<LP<?>, List<ResolveClassSet>> abstractLCPResolver;
    private ElementResolver<LP<?>, List<ResolveClassSet>> abstractNotEqualLCPResolver;
    private ElementResolver<LP<?>, List<ResolveClassSet>> indirectLCPResolver;
    private ElementResolver<LP<?>, List<ResolveClassSet>> directLocalsResolver;
    private ElementResolver<LP<?>, List<ResolveClassSet>> indirectLocalsResolver;

    private ElementResolver<LA<?>, List<ResolveClassSet>> directLAPResolver;
    private ElementResolver<LA<?>, List<ResolveClassSet>> abstractLAPResolver;
    private ElementResolver<LA<?>, List<ResolveClassSet>> abstractNotEqualLAPResolver;
    private ElementResolver<LA<?>, List<ResolveClassSet>> indirectLAPResolver;
    
    private ElementResolver<AbstractGroup, ?> groupResolver;
    private ElementResolver<NavigatorElement, ?> navigatorResolver;
    private ElementResolver<FormEntity, ?> formResolver;
    private ElementResolver<AbstractWindow, ?> windowResolver;
    private ElementResolver<ImplementTable, ?> tableResolver;
    private ElementResolver<CustomClass, ?> classResolver;
    private ElementResolver<MetaCodeFragment, Integer> metaCodeFragmentResolver;


    public ResolveManager(LogicsModule LM) {
        this.LM = LM;
        initializeResolvers();
    }

    private void initializeResolvers() {
        directLCPResolver = new LAPResolver<>(LM, new ModuleLPFinder(), true, false);
        abstractLCPResolver = new LAPResolver<>(LM, new ModuleAbstractLPFinder(), true, false);
        abstractNotEqualLCPResolver = new LAPResolver<>(LM, new ModuleAbstractLPFinder(), true, true);
        indirectLCPResolver = new LAPResolver<>(LM, new ModuleIndirectLPFinder(), false, false);
        directLocalsResolver = new LAPResolver<>(LM, new ModuleDirectLocalsFinder(), true, false);
        indirectLocalsResolver = new LAPResolver<>(LM, new ModuleIndirectLocalsFinder(), false, false);
        
        directLAPResolver = new LAPResolver<>(LM, new ModuleLAFinder(), true, false);
        abstractLAPResolver = new LAPResolver<>(LM, new ModuleAbstractLAFinder(), true, false);
        abstractNotEqualLAPResolver = new LAPResolver<>(LM, new ModuleAbstractLAFinder(), true, true);
        indirectLAPResolver = new LAPResolver<>(LM, new ModuleIndirectLAFinder(), false, false);
        
        groupResolver = new ElementResolver<>(LM, new ModuleGroupFinder());
        navigatorResolver = new ElementResolver<>(LM, new ModuleNavigatorElementFinder());
        formResolver = new ElementResolver<>(LM, new ModuleFormFinder());
        windowResolver = new ElementResolver<>(LM, new ModuleWindowFinder());
        tableResolver = new ElementResolver<>(LM, new ModuleTableFinder());
        classResolver = new ElementResolver<>(LM, new ModuleClassFinder());
        metaCodeFragmentResolver = new ElementResolver<>(LM, new ModuleMetaCodeFragmentFinder());
    }
    
    public LP<?> findProperty(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LP<?> property = null;
        if (!CompoundNameUtils.hasNamespace(compoundName)) {
            property = findLocalProperty(compoundName, params);
        }
        if (property == null) {
            property = findGlobalProperty(compoundName, params);
        }
        return property;
    }
    
    private LP<?> findLocalProperty(String name, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LP<?> property = directLocalsResolver.resolve(name, params);
        if (property == null) {
            property = indirectLocalsResolver.resolve(name, params);
        }
        return property;
    }

    private LP<?> findGlobalProperty(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LP<?> property = directLCPResolver.resolve(compoundName, params);
        if (property == null) {
            property = indirectLCPResolver.resolve(compoundName, params);
        }
        return property;
    }
    
    public LP<?> findAbstractProperty(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return getAbstractLCPResolver(prioritizeNotEquals).resolve(compoundName, params);    
    } 

    public LA<?> findAction(String compoundName, List<ResolveClassSet> params) throws ResolvingErrors.ResolvingError {
        LA<?> property = directLAPResolver.resolve(compoundName, params);
        if (property == null) {
            property = indirectLAPResolver.resolve(compoundName, params);
        }
        return property;
    }
    
    public LA<?> findAbstractAction(String compoundName, List<ResolveClassSet> params, boolean prioritizeNotEquals) throws ResolvingErrors.ResolvingError {
        return getAbstractLAPResolver(prioritizeNotEquals).resolve(compoundName, params);    
    } 
    
    public ValueClass findClass(String compoundName) throws ResolvingErrors.ResolvingError {
        return classResolver.resolve(compoundName);
    }

    public MetaCodeFragment findMetaCodeFragment(String compoundName, int paramCnt) throws ResolvingErrors.ResolvingError {
        return metaCodeFragmentResolver.resolve(compoundName, paramCnt);
    }

    public AbstractGroup findGroup(String compoundName) throws ResolvingErrors.ResolvingError {
        return groupResolver.resolve(compoundName);
    }    
    
    public AbstractWindow findWindow(String compoundName) throws ResolvingErrors.ResolvingError {
        return windowResolver.resolve(compoundName);
    }

    public NavigatorElement findNavigatorElement(String compoundName) throws ResolvingErrors.ResolvingError {
        return navigatorResolver.resolve(compoundName);
    }
    
    public FormEntity findForm(String compoundName) throws ResolvingErrors.ResolvingError {
        return formResolver.resolve(compoundName);
    }
    
    public ImplementTable findTable(String compoundName) throws ResolvingErrors.ResolvingError {
        return tableResolver.resolve(compoundName);
    }

    private ElementResolver<LP<?>, List<ResolveClassSet>> getAbstractLCPResolver(boolean prioritizeNotEquals) {
        if (prioritizeNotEquals) {
            return abstractNotEqualLCPResolver;
        } else {
            return abstractLCPResolver;
        }
    }

    private ElementResolver<LA<?>, List<ResolveClassSet>> getAbstractLAPResolver(boolean prioritizeNotEquals) {
        if (prioritizeNotEquals) {
            return abstractNotEqualLAPResolver;
        } else {
            return abstractLAPResolver;
        }
    }
}
