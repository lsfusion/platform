package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;

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
    
    protected IncrementSingleListSelectionModel(Object object, String field) {
        super(object, field);
    }
}
