package platform.server.logics.scripted;

import platform.base.BaseUtils;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.LogicsModule;
import platform.server.logics.linear.LAP;
import platform.server.logics.linear.LCP;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.ExecutionContext;
import platform.server.logics.property.PropertyInterface;
import platform.server.logics.property.actions.SystemActionProperty;

import java.sql.SQLException;
import java.util.Map;

/**
 * User: DAle
 * Date: 15.11.12
 * Time: 17:11
 */

public class EvalActionProperty<P extends PropertyInterface> extends SystemActionProperty {
    private final LCP<P> source;
    private final BusinessLogics<?> BL;
    private final Map<P, ClassPropertyInterface> mapSource;
    private static int counter = 0;

    public EvalActionProperty(String sID, String caption, BusinessLogics<?> BL, LCP<P> source) {
        super(sID, caption, source.getInterfaceClasses());
        mapSource = BaseUtils.buildMap(source.listInterfaces, interfaces);
        this.source = source;
        this.BL = BL;
    }

    private String getScript(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        Map<P, DataObject> sourceToData = BaseUtils.join(mapSource, context.getKeys());
        return (String) source.read(context, BaseUtils.mapList(source.listInterfaces, sourceToData).toArray(new DataObject[interfaces.size()]));
    }

    private String getUniqueName() {
        ++counter;
        return "UNIQUENAME" + counter; // todo [dale]: сделать нормально
    }

    private String wrapScript(String script) {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("MODULE ");
        strBuilder.append(getUniqueName());
        strBuilder.append(";\n");
        strBuilder.append("REQUIRE ");
        boolean isFirst = true;
        for (LogicsModule module : BL.getLogicModules()) {
            if (!isFirst) {
                strBuilder.append(", ");
            }
            isFirst = false;
            strBuilder.append(module.getName());
        }
        strBuilder.append(";\n");
        strBuilder.append(script);
        return strBuilder.toString();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException {
        String script = getScript(context);
        ScriptingLogicsModule module = new ScriptingLogicsModule(BL.LM, BL, wrapScript(script));
        module.initModuleDependencies();
        module.initModule();
        module.initAliases();
        module.initProperties();
        LAP<?> runAction = module.getLAPByName("run");
        if (runAction != null) {
            runAction.execute(context);
        }
    }
}
