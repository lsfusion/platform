package platform.server.classes;

import platform.server.classes.sets.ConcreteCustomClassSet;
import platform.server.logics.table.ObjectTable;

public class BaseClass extends AbstractCustomClass {

    public ObjectTable table;

    public final UnknownClass unknown;
    public final AbstractCustomClass named;
    public final CustomObjectClass objectClass;

    public BaseClass(String caption) {
        super(caption);
        table = new ObjectTable(this);
        unknown = new UnknownClass(this);
        named = new AbstractCustomClass("Объект с именем", this);
        objectClass = new CustomObjectClass(named);
    }

    @Override
    public BaseClass getBaseClass() {
        return this;
    }

    public ObjectClass findClassID(Integer idClass) {
        if(idClass==null) return unknown;

        return findClassID((int)idClass);
    }

    public ConcreteObjectClass findConcreteClassID(Integer idClass) {
        if(idClass==null) return unknown;

        return findConcreteClassID((int)idClass);
    }

    public ConcreteCustomClass getConcrete() {
        ConcreteCustomClassSet concrete = new ConcreteCustomClassSet();
        fillNextConcreteChilds(concrete);
        return concrete.get(0);
    }
}
