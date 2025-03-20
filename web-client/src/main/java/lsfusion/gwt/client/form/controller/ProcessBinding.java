package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.event.GBindingEnv;
import lsfusion.gwt.client.form.event.GBindingEvent;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.object.GGroupObject;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import static lsfusion.gwt.client.base.GwtClientUtils.nullEquals;

public class ProcessBinding {

    public static void processBinding(EventHandler handler, boolean preview, boolean isCell, boolean panel,
                                      ArrayList<GBindingEvent> bindingEvents, ArrayList<GFormController.Binding> bindings,
                                      Function<EventTarget, GGroupObject> bindingGroupObjectFunction,
                                      BiFunction<GBindingEnv, Boolean, Boolean> bindPreview,
                                      Function<GBindingEnv, Boolean> bindDialog,
                                      Function<GBindingEnv, Boolean> bindWindow,
                                      TriFunction<GBindingEnv, GGroupObject, Boolean, Boolean> bindGroup,
                                      BiFunction<GBindingEnv, Event, Boolean> bindEditing,
                                      BiFunction<GBindingEnv, Boolean, Boolean> bindShowing,
                                      TriFunction<GBindingEnv, Boolean, Boolean, Boolean> bindPanel,
                                      TriFunction<GBindingEnv, Boolean, Boolean, Boolean> bindCell,
                                      Runnable checkCommitEditing) {
        final EventTarget target = handler.event.getEventTarget();
        if (!Element.is(target)) {
            return;
        }

        Event event = handler.event;
        boolean isMouse = GMouseStroke.isEvent(event);
        TreeMap<Integer, GFormController.Binding> orderedBindings = new TreeMap<>(); // descending sorting by priority

        GGroupObject groupObject = bindingGroupObjectFunction.apply(target);
        for (int i = 0, size = bindingEvents.size(); i < size; i++) {
            GBindingEvent bindingEvent = bindingEvents.get(i);
            if (bindingEvent.event.check(event)) {
                GFormController.Binding binding = bindings.get(i);
                boolean equalGroup;
                GBindingEnv bindingEnv = bindingEvent.env;
                if(bindPreview.apply(bindingEnv, preview) &&
                        bindDialog.apply(bindingEnv) &&
                        bindWindow.apply(bindingEnv) &&
                        bindGroup.apply(bindingEnv, groupObject, equalGroup = nullEquals(groupObject, binding.groupObject)) &&
                        bindEditing.apply(bindingEnv, event) &&
                        bindShowing.apply(bindingEnv, binding.showing()) &&
                        bindPanel.apply(bindingEnv, isMouse, panel) &&
                        bindCell.apply(bindingEnv, isMouse, isCell)) {
                    orderedBindings.put(-(GwtClientUtils.nvl(bindingEnv.priority, i) + (equalGroup ? 100 : 0)), binding); // increasing priority for group object
                    bindingEvent.event.check(event);
                }
            }
        }

        for (GFormController.Binding binding : orderedBindings.values()) {
            if (binding.enabled()) {
                checkCommitEditing.run();
                handler.consume();

                binding.exec(event);
                return;
            }
        }
    }

}
