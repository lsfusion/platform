package lsfusion.client.form.controller;

import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.interop.form.event.InputEvent;
import lsfusion.interop.form.event.MouseInputEvent;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProcessBinding {

    public static void processBinding(InputEvent ks, boolean preview, java.awt.event.InputEvent ke, Supplier<ClientGroupObject> groupObjectSupplier, boolean panel,
                                  BiFunction<ClientGroupObject, ClientFormController.Binding, Boolean> equalGroupFunction,
                                  Map<InputEvent, List<ClientFormController.Binding>> bindings, List<ClientFormController.Binding> keySetBindings,
                                  BiFunction<ClientFormController.Binding, Boolean, Boolean> bindPreview,
                                  Function<ClientFormController.Binding, Boolean> bindDialog,
                                  Function<ClientFormController.Binding, Boolean> bindWindow,
                                  BiFunction<ClientGroupObject, ClientFormController.Binding, Boolean> bindGroup,
                                  BiFunction<ClientFormController.Binding, java.awt.event.InputEvent, Boolean> bindEditing,
                                  Function<ClientFormController.Binding, Boolean> bindShowing,
                                  BiFunction<ClientFormController.Binding, Boolean, Boolean> bindPanel,
                                  Runnable commitOrCancelCurrentEditing) {
        List<ClientFormController.Binding> keyBinding = bindings.getOrDefault(ks, ks instanceof MouseInputEvent ? null : keySetBindings);
        if (keyBinding != null && !keyBinding.isEmpty()) { // optimization
            TreeMap<Integer, ClientFormController.Binding> orderedBindings = new TreeMap<>();

            // increasing priority for group object
            ClientGroupObject groupObject = groupObjectSupplier.get();
            for (ClientFormController.Binding binding : keyBinding) // descending sorting by priority
                if ((binding.isSuitable == null || binding.isSuitable.apply(ke)) && bindPreview.apply(binding, preview)
                        && bindDialog.apply(binding) && bindWindow.apply(binding) && bindGroup.apply(groupObject, binding)
                        && bindEditing.apply(binding, ke) && bindShowing.apply(binding) && bindPanel.apply(binding, panel))
                    // increasing priority for group object; possible problems in forms with over 1000 properties
                    orderedBindings.put(-(binding.priority + (equalGroupFunction.apply(groupObject, binding) ? 1000 : 0)), binding);

            if (!orderedBindings.isEmpty())
                commitOrCancelCurrentEditing.run();

            for (ClientFormController.Binding binding : orderedBindings.values()) {
                if (binding.pressed(ke)) {
                    ke.consume();
                    return;
                }
            }
        }
    }

}
