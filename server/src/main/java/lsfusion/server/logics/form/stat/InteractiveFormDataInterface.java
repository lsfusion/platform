package lsfusion.server.logics.form.stat;

import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.interop.form.user.FormUserPreferences;
import lsfusion.server.data.ObjectValue;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.type.Type;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.BaseClass;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.instance.object.GroupObjectInstance;
import lsfusion.server.logics.form.interactive.instance.object.ObjectInstance;
import lsfusion.server.logics.form.interactive.instance.order.OrderInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyDrawInstance;
import lsfusion.server.logics.form.interactive.instance.property.PropertyObjectInstance;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.logics.form.struct.order.CompareEntity;
import lsfusion.server.logics.form.struct.property.PropertyDrawEntity;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;

import java.sql.SQLException;

public class InteractiveFormDataInterface extends AbstractFormDataInterface {

    protected final FormInstance form;
    protected final Integer groupId;

    protected final FormUserPreferences preferences; // now it is not stored on the server, so we need to pass it from the client 

    public InteractiveFormDataInterface(FormInstance form, Integer groupId, FormUserPreferences preferences) {
        this.form = form;
        this.groupId = groupId;
        this.preferences = preferences;
    }

    @Override
    public FormEntity getFormEntity() {
        return form.entity;
    }

    @Override
    public QueryEnvironment getQueryEnv() {
        return form.getQueryEnv();
    }

    @Override
    public DataSession getSession() {
        return form.getSession();
    }

    @Override
    public BaseClass getBaseClass() {
        return form.BL.LM.baseClass;
    }

    @Override
    public Modifier getModifier() {
        return form.getModifier();
    }

    protected GroupObjectInstance getInstance(GroupObjectEntity groupObject, FormInstance form) {
        return form.instanceFactory.getInstance(groupObject);
    }

    protected ObjectInstance getInstance(ObjectEntity value, FormInstance form) {
        return form.instanceFactory.getInstance(value);
    }

    protected PropertyDrawInstance getInstance(PropertyDrawEntity value, FormInstance form) {
        return form.instanceFactory.getInstance(value);
    }

    protected PropertyObjectInstance getInstance(PropertyObjectEntity value, FormInstance form) {
        return form.instanceFactory.getInstance(value);
    }

    @Override
    public GetKeyValue<ImOrderSet<PropertyDrawEntity>, GroupObjectEntity, ImOrderSet<PropertyDrawEntity>> getUserVisible() {
        return new GetKeyValue<ImOrderSet<PropertyDrawEntity>, GroupObjectEntity, ImOrderSet<PropertyDrawEntity>>() {
            public ImOrderSet<PropertyDrawEntity> getMapValue(GroupObjectEntity key, ImOrderSet<PropertyDrawEntity> value) {
                return form.getVisibleProperties(getInstance(key, form), value, preferences);
            }
        };
    }

    @Override
    public Where getWhere(GroupObjectEntity groupObject, ImSet<GroupObjectEntity> valueGroups, ImMap<ObjectEntity, Expr> mapExprs) throws SQLException, SQLHandledException {
        GroupObjectInstance groupInstance = getInstance(groupObject, form);
        ImMap<ObjectInstance, Expr> mapInstanceExprs = mapExprs.mapKeys(new GetValue<ObjectInstance, ObjectEntity>() {
            public ObjectInstance getMapValue(ObjectEntity value) {
                return getInstance(value, form);
            }
        });
        return groupInstance.getWhere(mapInstanceExprs, form.getModifier());
    }

    @Override
    public ImOrderMap<CompareEntity, Boolean> getOrders(GroupObjectEntity groupObject, ImSet<GroupObjectEntity> valueGroups) {
        GroupObjectInstance groupInstance = getInstance(groupObject, form);
        return groupInstance.orders.mapOrderKeys(new GetValue<CompareEntity, OrderInstance>() {
            public CompareEntity getMapValue(final OrderInstance value) {
                return new CompareEntity() {
                    public Type getType() {
                        return value.getType();
                    }
                    public Expr getEntityExpr(ImMap<ObjectEntity, ? extends Expr> mapExprs, Modifier modifier) throws SQLException, SQLHandledException {
                        return value.getExpr(mapExprs.mapKeys(new GetValue<ObjectInstance, ObjectEntity>() {
                            public ObjectInstance getMapValue(ObjectEntity value) {
                                return getInstance(value, form);
                            }
                        }), modifier);
                    }
                };
            }
        });
    }

    @Override
    protected ObjectValue getValueObject(ObjectEntity object) {
        return getInstance(object, form).getObjectValue();
    }

    @Override
    protected ImSet<ObjectEntity> getValueObjects() {
        return getFormEntity().getObjects().filterFn(new SFunctionSet<ObjectEntity>() {
            public boolean contains(ObjectEntity element) {
                return !getInstance(element.groupTo, form).curClassView.isGrid();
            }
        });
    }

    @Override
    public StaticDataGenerator.Hierarchy getHierarchy(boolean isReport) {
        GroupObjectHierarchy groupObjectHierarchy;
                
        FormEntity formEntity = getFormEntity();
        if(groupId != null) {
            GroupObjectEntity groupObject = formEntity.getGroupObject(groupId);
            groupObjectHierarchy = formEntity.getSingleGroupObjectHierarchy(groupObject);
            return new StaticDataGenerator.Hierarchy(groupObjectHierarchy, MapFact.singleton(groupObject, form.getPropertyEntitiesShownInGroup(getInstance(groupObject, form))), getFormEntity().getGroupsList().getSet().filterFn(new SFunctionSet<GroupObjectEntity>() {
                @Override
                public boolean contains(GroupObjectEntity element) {
                    return !groupId.equals(element.getID());
                }
            }));
        }
        return super.getHierarchy(isReport);
    }
}
