package platform.server.data.classes.where;

import platform.base.QuickSet;
import platform.server.data.classes.CustomClass;

public class CustomClassSet extends QuickSet<CustomClass,CustomClassSet> {

    public CustomClassSet() {
    }

    protected CustomClass[] newArray(int size) {
        return new CustomClass[size];
    }

    protected CustomClassSet getThis() {
        return this;
    }

    ConcreteCustomClassSet toConcrete() {
        ConcreteCustomClassSet result = new ConcreteCustomClassSet();
        for(int i=0;i<size;i++)
            table[indexes[i]].fillNextConcreteChilds(result);
        return result;
    }
}
