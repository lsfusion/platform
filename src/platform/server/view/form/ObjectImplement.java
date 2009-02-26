package platform.server.view.form;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.types.Type;
import platform.server.logics.classes.RemoteClass;

import java.util.Map;
import java.util.Set;

// на самом деле нужен collection но при extend'е нужна конкретная реализация
public class ObjectImplement {

    public ObjectImplement(int iID, RemoteClass iBaseClass, String iCaption, GroupObjectImplement groupObject) {
        this(iID, iBaseClass, iCaption);

        groupObject.addObject(this);
    }

    public ObjectImplement(int iID, RemoteClass iBaseClass, String iCaption) {
        ID = iID;
        baseClass = iBaseClass;
        gridClass = baseClass;
        caption = iCaption;
    }

    public ObjectImplement(int iID, RemoteClass iBaseClass) {
        this(iID, iBaseClass, "");
    }

    // выбранный объект, класс выбранного объекта
    public Integer idObject = null;
    public RemoteClass Class = null;

    public RemoteClass baseClass;
    // выбранный класс
    public RemoteClass gridClass;

    // 0 !!! - изменился объект, 1 !!! - класс объекта, 3 !!! - класса, 4 - классовый вид

    public static int UPDATED_OBJECT = (1);
    public static int UPDATED_CLASS = (1 << 1);
    public static int UPDATED_GRIDCLASS = (1 << 3);

    public int updated = UPDATED_GRIDCLASS;

    public GroupObjectImplement groupTo;

    public String caption = "";

    public String toString() {
        return caption;
    }

    // идентификатор (в рамках формы)
    public int ID = 0;

    // символьный идентификатор, нужен для обращению к свойствам в печатных формах
    public String sID;
    public String getSID() {
        if (sID != null) return sID; else return "obj" + ID;
    }

    SourceExpr getSourceExpr(Set<GroupObjectImplement> ClassGroup, Map<ObjectImplement, ? extends SourceExpr> ClassSource) {
        return (ClassGroup!=null && ClassGroup.contains(groupTo)?ClassSource.get(this): Type.object.getExpr(idObject));
    }
}
