package platform.server.logics.property.actions;

import platform.interop.action.ClientAction;
import platform.server.data.expr.Expr;
import platform.server.data.expr.where.CompareWhere;
import platform.server.data.where.Where;
import platform.server.form.entity.GroupObjectEntity;
import platform.server.form.instance.FormInstance;
import platform.server.form.instance.GroupObjectInstance;
import platform.server.form.instance.PropertyObjectInterfaceInstance;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.linear.LP;
import platform.server.logics.property.ActionProperty;
import platform.server.logics.property.ClassPropertyInterface;
import platform.server.logics.property.Property;
import platform.server.logics.property.PropertyInterface;
import platform.server.session.DataSession;
import platform.server.session.PropertyChange;

import java.sql.SQLException;
import java.util.*;

import static platform.base.BaseUtils.*;
import static platform.server.logics.PropertyUtils.getValueClasses;

public class GroupChangeActionProperty extends ActionProperty {

    private final GroupObjectEntity filterGroupObject;
    private final Property getterProperty;
    private final Property mainProperty;

    private HashSet<PropertyInterface> grouping;

    private Map<PropertyInterface, PropertyInterface> mapGetToMain;
    private Map<PropertyInterface, ClassPropertyInterface> mapGetToThis;

    private Map<PropertyInterface, ClassPropertyInterface> mapMainToThis;

    /**
     * @param mainLP - свойство, куда будем писать
     * @param getterLP - свойство, из которого будем читать значение
     */
    public GroupChangeActionProperty(String sID, String caption, GroupObjectEntity filterGroupObject, LP<?> mainLP, int[] groupInts, LP<?> getterLP, int[] getterInts) {
        super(sID, caption, getValueClasses(false, new LP[]{mainLP, getterLP}, new int[][]{null, getterInts}));

        this.filterGroupObject = filterGroupObject;
        this.mainProperty = mainLP.property;
        this.getterProperty = getterLP.property;

        this.mapGetToMain = new HashMap<PropertyInterface, PropertyInterface>();
        this.mapGetToThis = new HashMap<PropertyInterface, ClassPropertyInterface>();
        this.mapMainToThis = new HashMap<PropertyInterface, ClassPropertyInterface>();

        List<ClassPropertyInterface> listInterfaces = (List<ClassPropertyInterface>) interfaces;

        for (int i = 0; i < getterInts.length; ++i) {
            int gi = getterInts[i];
            if (gi < mainLP.listInterfaces.size()) {
                mapGetToMain.put(getterLP.listInterfaces.get(i), mainLP.listInterfaces.get(gi));
            }
            mapGetToThis.put(getterLP.listInterfaces.get(i), listInterfaces.get(gi));
        }

        this.grouping = new HashSet<PropertyInterface>();
        for (int mi : groupInts) {
            grouping.add(mainLP.listInterfaces.get(mi));
            mapMainToThis.put(mainLP.listInterfaces.get(mi), listInterfaces.get(mi));
        }
    }

    public void execute(Map<ClassPropertyInterface, DataObject> keys, ObjectValue value, List<ClientAction> actions, RemoteForm executeForm, Map<ClassPropertyInterface, PropertyObjectInterfaceInstance> mapObjects) throws SQLException {
        FormInstance<?> form = (FormInstance<?>) executeForm.form;
        DataSession session = form.session;

        Map<PropertyInterface, Expr> mainKeys = mainProperty.getMapKeys();

        Where changeWhere = CompareWhere.compareValues(
                filterNotKeys(mainKeys, grouping),
                join(mapMainToThis, keys)
        );

        if (filterGroupObject != null) {
            GroupObjectInstance groupInstance = form.instanceFactory.getInstance(filterGroupObject);
            changeWhere = changeWhere.and(
                    groupInstance.getWhere(
                            filterKeys(crossJoin(join(mapMainToThis, mapObjects), mainKeys), groupInstance.objects),
                            session.modifier));
        }

        Map<PropertyInterface, Expr> getterKeys = new HashMap<PropertyInterface, Expr>();
        for (PropertyInterface getIFace : (List<PropertyInterface>) getterProperty.interfaces) {
            if (mapGetToMain.containsKey(getIFace)) {
                getterKeys.put(getIFace, mainKeys.get(mapGetToMain.get(getIFace)));
            } else {
                getterKeys.put(getIFace, keys.get(mapGetToThis.get(getIFace)).getExpr());
            }
        }

        Expr setExpr = getterProperty.getExpr(getterKeys, session.modifier);

        PropertyChange mainPropertyChange = new PropertyChange(mainKeys, setExpr, changeWhere);

        actions.addAll(
                session.execute(mainProperty, mainPropertyChange, session.modifier, executeForm, join(mapMainToThis, mapObjects))
        );
    }
}
