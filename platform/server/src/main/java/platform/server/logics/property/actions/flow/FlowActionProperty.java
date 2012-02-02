package platform.server.logics.property.actions.flow;

import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static platform.base.BaseUtils.join;
import static platform.base.BaseUtils.nullJoin;
import static platform.server.logics.PropertyUtils.getValueClasses;

public class FlowActionProperty extends ActionProperty {
    private final LP[] actions;

    private final Map<PropertyInterface, ClassPropertyInterface>[] mapActions;
    private final PropertyMapImplement<PropertyInterface, ClassPropertyInterface>[] mapActionImplements = null;

    public FlowActionProperty(String sID, String caption, LP[] actions, int[][] imapActions) {
        super(sID, caption, getValueClasses(false, actions, imapActions));

        assert actions.length == imapActions.length;

        this.actions = actions;
        this.mapActions = new Map[actions.length];

        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>) interfaces;

        for (int i = 0; i < actions.length; ++i) {
            LP<?> action = actions[i];
            int[] imapAction = imapActions[i];

            //todo: ??? нужно ли кидать Exception для более жёсткого assertion
            assert action.property instanceof ActionProperty;

            Map<PropertyInterface, ClassPropertyInterface> mapAction = new HashMap<PropertyInterface, ClassPropertyInterface>();
            for (int j = 0; j < imapAction.length; ++j) {
                mapAction.put(action.listInterfaces.get(j), listInterfaces.get(imapAction[j]));
            }

            mapActions[i] = mapAction;
        }
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        for (int i = 0; i < actions.length; ++i) {
            LP action = actions[i];
            Map<PropertyInterface, ClassPropertyInterface> mapAction = mapActions[i];

            Map<PropertyInterface, DataObject> mapKeys = join(mapAction, context.getKeys());

            Map<PropertyInterface, PropertyObjectInterfaceInstance> mapObjects = nullJoin(mapAction, context.getObjectInstances());

            context.addActions(
                    action.property.execute(
                            mapKeys,
                            context.getSession(),
                            true,
                            context.getModifier(),
                            context.getRemoteForm(),
                            mapObjects
                    ));
        }
    }
}
