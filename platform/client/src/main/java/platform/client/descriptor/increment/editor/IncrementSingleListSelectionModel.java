package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;
import platform.interop.context.ApplicationContext;
import platform.interop.context.ApplicationContextHolder;
import platform.interop.context.ApplicationContextProvider;

import javax.swing.*;
import java.util.List;
import java.util.Collections;

public abstract class IncrementSingleListSelectionModel extends IncrementListSelectionModel<Object> implements ComboBoxModel {

    protected void updateSelectionViews() {
        fireContentsChanged(this, -1, -1);
    }

    public List<?> getList() {
        List<?> singleList = getSingleList();
        if(allowNulls())
            return BaseUtils.mergeList(Collections.singletonList(null), singleList);
        else
            return singleList;
    }

    public boolean allowNulls() {
        return false;
    }

    public abstract List<?> getSingleList();
    
    protected IncrementSingleListSelectionModel(ApplicationContextProvider object, String field) {
        super(object, field);
    }
}
