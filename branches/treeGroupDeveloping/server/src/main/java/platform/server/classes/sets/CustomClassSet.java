package platform.server.classes.sets;

import platform.base.QuickSet;
import platform.server.classes.CustomClass;

public class CustomClassSet extends QuickSet<CustomClass> {

    public CustomClassSet() {
    }

    ConcreteCustomClassSet toConcrete() {
        ConcreteCustomClassSet result = new ConcreteCustomClassSet();
        for(int i=0;i<size;i++)
            get(i).fillNextConcreteChilds(result);
        return result;
    }
}
