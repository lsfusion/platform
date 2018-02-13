package lsfusion.server.classes;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.mutables.Version;

public class AbstractCustomClass extends CustomClass {

    public AbstractCustomClass(String sID, LocalizedString caption, Version version, CustomClass... parents) {
        super(sID, caption, version, parents);
    }

    public void fillNextConcreteChilds(MSet<ConcreteCustomClass> mClassSet) {
        for(CustomClass child : getChildrenIt())
            child.fillNextConcreteChilds(mClassSet);
    }

    public static ConcreteCustomClass getSingleClass(CustomClass[] children) {
        ConcreteCustomClass single = null;
        for(CustomClass child : children) {
            ConcreteCustomClass childSingle = child.getSingleClass();
            if(childSingle==null) return null; // если несколько single то возвращаем null
            if(single==null)
                single = childSingle;
            else
                if(!childSingle.equals(single)) return null; // если другой single то
        }
        return single;
    }

    public ConcreteCustomClass getSingleClass() {
        ImSet<CustomClass> children = getChildren();
        return getSingleClass(children.toList().toArray(new CustomClass[children.size()]));
    }

}
