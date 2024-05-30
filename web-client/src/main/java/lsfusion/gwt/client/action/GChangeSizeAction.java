package lsfusion.gwt.client.action;

import java.util.List;
import java.util.Map;

public class GChangeSizeAction extends GExecuteAction {
    public Map<String, String> resources;
    public List<String> unloadResources;

    public GChangeSizeAction() {
    }

    public GChangeSizeAction(Map<String, String> resources, List<String> unloadResources) {
        this.resources = resources;
        this.unloadResources = unloadResources;
    }

    @Override
    public void execute(GActionDispatcher dispatcher) {
        dispatcher.execute(this);
    }
}