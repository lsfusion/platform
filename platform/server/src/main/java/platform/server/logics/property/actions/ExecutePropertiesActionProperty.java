package platform.server.logics.property.actions;

import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.logics.DataObject;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.join;
import static platform.server.logics.PropertyUtils.getValueClasses;

public class ExecutePropertiesActionProperty extends ActionProperty {
    public final static int EPA_INTERFACE = 0; // значение идет доп. интерфейсом
    public final static int EPA_DEFAULT = 1; // писать из getDefaultValue
    public final static int EPA_NULL = 2; // писать null

    private final int writeType;

    private final LP[] dataProperties;
    private final HashSet<Property> changedProps;

    private final Map<PropertyInterface, ClassPropertyInterface>[] mapInterfaces;
    private final ClassPropertyInterface[] mapResults;

    /**
     * Если writeType == false, то сначала идут номера для интерфейсов, потом - для входа
     * @param imapInterfaces должны быть 0-based
     */
    public ExecutePropertiesActionProperty(String sID, String caption, int writeType, LP[] dataProperties, int[][] imapInterfaces) {
        super(sID, caption, getValueClasses(writeType == EPA_INTERFACE, dataProperties, imapInterfaces));

        this.dataProperties = dataProperties;
        this.writeType = writeType;
        this.mapInterfaces = new Map[dataProperties.length];
        this.mapResults = writeType == EPA_INTERFACE ? new ClassPropertyInterface[dataProperties.length] : null;

        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>)interfaces;

        for (int i = 0; i < dataProperties.length; ++i) {
            LP dataProperty = dataProperties[i];
            List<? extends PropertyInterface> propInterfaces = dataProperty.listInterfaces;
            int[] imapPropInterfaces = imapInterfaces[i];

            Map<PropertyInterface, ClassPropertyInterface> mapPropInterfaces = new HashMap<PropertyInterface, ClassPropertyInterface>();
            for (int j = 0; j < imapPropInterfaces.length; ++j) {
                ClassPropertyInterface mapInterface = listInterfaces.get(imapPropInterfaces[j]);

                if (writeType == EPA_INTERFACE && j == imapPropInterfaces.length - 1) {
                    mapResults[i] = mapInterface;
                } else {
                    mapPropInterfaces.put(propInterfaces.get(j), mapInterface);
                }
            }

            mapInterfaces[i] = mapPropInterfaces;
        }

        changedProps = new HashSet<Property>();
        for (LP prop : dataProperties) {
            changedProps.add(prop.property);
        }
    }

    @Override
    public Set<Property> getChangeProps() {
        return changedProps;
    }

    @Override
    public void execute(ExecutionContext context) throws SQLException {
        for (int i = 0; i < dataProperties.length; ++i) {
            LP dataProperty = dataProperties[i];
            Map<PropertyInterface, ClassPropertyInterface> mapPropInterfaces = mapInterfaces[i];

            Object execValue;
            switch (writeType) {
                case EPA_INTERFACE :
                    execValue = context.getKeyValue(mapResults[i]).getValue();
                    break;
                case EPA_DEFAULT :
                    execValue = dataProperty.property.getCommonClasses().value.getDefaultValue();
                    break;
                case EPA_NULL :
                    execValue = null;
                    break;
                default :
                    throw new RuntimeException("Unknown writeType");
            }

            Map<PropertyInterface, DataObject> mapKeys = join(mapPropInterfaces, context.getKeys());

            boolean inForm = context.getRemoteForm() != null && dataProperty.property instanceof UserProperty;
            if (inForm) {
                Map<PropertyInterface, PropertyObjectInterfaceInstance> mapObjects = join(mapPropInterfaces, context.getObjectInstances());
                context.addActions(
                        dataProperty.property.execute(
                                mapKeys,
                                context.getSession(),
                                execValue,
                                context.getModifier(),
                                context.getRemoteForm(),
                                mapObjects));
            } else {
                context.addActions(dataProperty.execute(execValue, context, mapKeys));
            }
        }
    }
}
