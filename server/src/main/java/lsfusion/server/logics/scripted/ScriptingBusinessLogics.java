package lsfusion.server.logics.scripted;

import lsfusion.server.logics.BusinessLogics;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static lsfusion.base.BaseUtils.isRedundantString;

public class ScriptingBusinessLogics extends BusinessLogics<ScriptingBusinessLogics> {
    private List<String> scriptFilePaths;
    private List<String> excludedScriptFilePaths;

    public void setScriptFilePaths(String scriptFilePaths) {
        if (!isRedundantString(scriptFilePaths)) {
            this.scriptFilePaths = asList(scriptFilePaths.split(";"));
        } else {
            this.scriptFilePaths = defaultIncludedScriptPaths;
        }
    }

    public void setExcludedScriptFilePaths(String excludedScriptFilePaths) {
        if (!isRedundantString(excludedScriptFilePaths)) {
            this.excludedScriptFilePaths = asList(excludedScriptFilePaths.split(";"));
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        if (scriptFilePaths == null) {
            scriptFilePaths = Collections.emptyList();
        }

        if (excludedScriptFilePaths == null) {
            excludedScriptFilePaths = Collections.emptyList();
        }
    }

    @Override
    public void createModules() throws IOException {
        super.createModules();
        addModulesFromResource(scriptFilePaths, excludedScriptFilePaths);
    }
}
