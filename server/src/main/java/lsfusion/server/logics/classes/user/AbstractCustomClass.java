package lsfusion.server.logics.classes.user;

import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.base.version.Version;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class AbstractCustomClass extends CustomClass {

    public AbstractCustomClass(String canonicalName, LocalizedString caption, String image, Version version, ImList<CustomClass> parents) {
        super(canonicalName, caption, image, version, parents);
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
