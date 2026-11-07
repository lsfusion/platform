package lsfusion.tests;

import com.google.common.base.Throwables;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.language.ScriptingErrorLog;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.data.StoredDataProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;
import java.sql.SQLException;

import static lsfusion.interop.action.ServerResponse.CHANGE;

// two modules that do not require each other giving one property actions (ASON CHANGE, ASON KEYPRESS of one key, ASON
// CONTEXTMENU), the later one in the module order writing first, as the parse threads' timing can have it : the property
// keeps the later module's change action and key, and its context menu goes as a parse of one module after the other
// had it - the earlier module binding pkX and pkY, the later one pkZ and pkX again : pkX, pkY, pkZ, pkX staying as the
// earlier module bound it, as an item bound again does on a form.
// A property the server started with keeps its bindings final, so the one given them is made here. What the test saw
// goes into pkOrderFailure
public class BindPropertyInModuleOrderAction extends AtOnceAction {

    public BindPropertyInModuleOrderAction(ScriptingLogicsModule LM, ValueClass... classes) {
        super(LM, classes);
    }

    @Override
    public void executeInternal(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        try {
            ActionMapImplement earlierAction = findAction("pkAction[]").action.getImplement();
            ActionMapImplement laterAction = findAction("pkLaterAction[]").action.getImplement();
            String earlier = earlierAction.action.getSID();
            String later = laterAction.action.getSID();
            LogicsModule[] modules = independentModules(context.getBL(), LM);

            StoredDataProperty property = new StoredDataProperty(LocalizedString.NONAME, new ValueClass[0], StringClass.text);
            property.setEventAction(CHANGE, laterAction, modules[1].getVersion());
            property.setEventAction(CHANGE, earlierAction, modules[0].getVersion());
            property.setKeyAction(KeyStroke.getKeyStroke("F8"), later, modules[1].getVersion());
            property.setKeyAction(KeyStroke.getKeyStroke("F8"), earlier, modules[0].getVersion());
            property.setContextMenuAction("pkZ", null, LocalizedString.create("later z", false), modules[1].getVersion());
            property.setContextMenuAction("pkX", null, LocalizedString.create("later x", false), modules[1].getVersion());
            property.setContextMenuAction("pkX", null, LocalizedString.create("earlier x", false), modules[0].getVersion());
            property.setContextMenuAction("pkY", null, LocalizedString.create("earlier y", false), modules[0].getVersion());

            String failure = null;
            ActionMapImplement<?, ?> change = property.getExplicitEventAction(CHANGE);
            if (change != laterAction)
                failure = "the property kept the change action " + (change == earlierAction ? "of the module earlier in the module order (" + modules[0].getName() + "), written last" : change);
            String key = property.getKeyBindings().get(KeyStroke.getKeyStroke("F8"));
            if (failure == null && !later.equals(key))
                failure = "the property kept the key bound to " + key;
            ImOrderMap<String, ActionOrProperty.ContextMenuBinding> menu = property.getContextMenuBindings();
            ImOrderSet<String> items = menu.keyOrderSet();
            int x = items.indexOf("pkX");
            if (failure == null && !(x >= 0 && x + 1 < items.size() - 1 && "pkY".equals(items.get(x + 1)) && "pkZ".equals(items.get(x + 2))))
                failure = "the property has its context menu in the order " + items + ", not pkX, pkY, pkZ";
            if (failure == null && !"earlier x".equals(menu.get("pkX").caption.getSourceString()))
                failure = "the property has the context menu item pkX of " + menu.get("pkX").caption.getSourceString() + ", not as it was bound first";

            findProperty("pkOrderFailure[]").change(failure, context);
        } catch (ScriptingErrorLog.SemanticErrorException e) {
            throw Throwables.propagate(e);
        }
    }
}
