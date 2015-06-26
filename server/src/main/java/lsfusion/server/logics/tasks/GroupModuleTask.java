package lsfusion.server.logics.tasks;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.LogicsModule;
import org.antlr.runtime.RecognitionException;

import java.util.*;

public abstract class GroupModuleTask extends GroupSingleTask<LogicsModule> {

    protected abstract boolean isGraph();

    protected abstract void runTask(LogicsModule module) throws RecognitionException;

    protected long getTaskComplexity(LogicsModule module) {
        return 1;
    }

    protected List<LogicsModule> getElements() {
        BusinessLogics<?> bl = (BusinessLogics<?>) getBL();
        assert getDependElements(bl.LM).isEmpty();
        return bl.getLogicModules();
    }

    protected String getElementCaption(LogicsModule element, int all, int current) {
        return "module : " + element.getLogName(all, current);
    }

    protected String getElementCaption(LogicsModule element) {
        return element.getName();
    }

    protected String getErrorsDescription(LogicsModule element) {
        return element.getErrorsDescription();
    }

    protected ImSet<LogicsModule> getDependElements(LogicsModule key) {
        BaseLogicsModule<? extends BusinessLogics<?>> rootElement = ((BusinessLogics<?>) getBL()).LM;
        if(key.equals(rootElement))
            return SetFact.EMPTY();

        ImSet<LogicsModule> result;
        if(isGraph()) {
            result = SetFact.fromJavaSet(key.getRequiredModules()).mapSetValues(new GetValue<LogicsModule, String>() {
                public LogicsModule getMapValue(String value) {
                    return ((BusinessLogics<?>) getBL()).getSysModule(value);
                }
            });
        } else
            result = SetFact.EMPTY();

        if (result.isEmpty()) {
            result = SetFact.<LogicsModule>singleton(rootElement);
        }
        return result;
    }
}
