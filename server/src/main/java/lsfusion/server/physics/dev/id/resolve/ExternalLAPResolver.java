package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.user.CustomClass;

import java.util.Collection;
import java.util.List;

// what a name that comes from outside the code - the one a call names, of the controller or of the Action API - is
// looked for in : the whole logics (BusinessLogics), or the logics as the code of a customization sees it
// (EvalScriptingLogicsModule). Unlike a name in the code (ResolveManager), it is looked for in every namespace alike,
// with no priority of the module's own
public interface ExternalLAPResolver {

    LA<?> findActionByName(String name);

    LP<?> findPropertyByName(String name);

    // the modules it is looked for in, by namespace (BusinessLogicsResolvingUtils.findLAPByCompoundName)
    Collection<String> getNamespacesList();

    List<LogicsModule> getNamespaceModules(String namespace);

    // a class a signature in the name gives (PropertyCompoundNameParser)
    CustomClass findClassByCompoundName(String compoundName);

    // a name the compound one finds nothing by, as an external id
    LA<?> findActionByExtId(String extId);

    LP<?> findPropertyByExtId(String extId);
}
