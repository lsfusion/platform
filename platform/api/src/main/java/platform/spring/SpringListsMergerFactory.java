package platform.spring;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import java.util.ArrayList;
import java.util.List;

public class SpringListsMergerFactory extends AbstractFactoryBean<List> {

    private List<List> listsToMerge;

    public void setListsToMerge(List<List> listsToMerge) {
        this.listsToMerge = listsToMerge;
    }

    @Override
    public Class<List> getObjectType() {
        return List.class;
    }

    @Override
    protected List createInstance() throws Exception {
        if (listsToMerge == null || listsToMerge.size() == 0) {
            return null;
        }
        List result = new ArrayList();
        for (List list : listsToMerge) {
            result.addAll(list);
        }
        return result;
    }
}
