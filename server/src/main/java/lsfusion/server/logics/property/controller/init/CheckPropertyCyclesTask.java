package lsfusion.server.logics.property.controller.init;

import lsfusion.server.logics.controller.init.SimpleBLTask;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.physics.admin.Settings;
import org.apache.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class CheckPropertyCyclesTask extends SimpleBLTask {

    private final Set<Property<?>> globalMarksWithoutPrev = ConcurrentHashMap.newKeySet();
    private final Set<Property<?>> globalMarksWithPrev = ConcurrentHashMap.newKeySet();

    @Override
    public String getCaption() {
        return "Checking property dependency cycles";
    }

    @Override
    public void run(Logger logger) {
        if (!Settings.get().isDryRun())
            return;

        for (Property<?> property : getBL().getProperties()) {
            property.getRecDepends();

            if (property instanceof CaseUnionProperty && ((CaseUnionProperty) property).isAbstract()) {
                // run it twice to eliminate loops that contain both PREV and events/CHANGED simultaneously
                ((CaseUnionProperty) property).checkRecursions(globalMarksWithoutPrev, false);
                ((CaseUnionProperty) property).checkRecursions(globalMarksWithPrev, true);
            }
        }
    }
}
