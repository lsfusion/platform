package lsfusion.server.language;

import lsfusion.server.logics.BusinessLogics;

import java.io.IOException;
import java.util.List;

import static java.util.Arrays.asList;

public class ScriptingBusinessLogics extends BusinessLogics {
    private List<String> includePaths;
    private List<String> excludePaths;

    public void setIncludePaths(String includePaths) {
        this.includePaths = asList(includePaths.split(";"));
    }

    public void setExcludePaths(String excludePaths) {
        this.excludePaths = asList(excludePaths.split(";"));
    }

    @Override
    public void createModules() throws IOException {
        super.createModules();
        addModulesFromResource(includePaths, excludePaths);
    }
}
