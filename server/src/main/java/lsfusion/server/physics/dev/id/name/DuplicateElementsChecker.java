package lsfusion.server.physics.dev.id.name;

import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.classes.CustomClass;
import lsfusion.server.logics.form.struct.FormEntity;
import lsfusion.server.logics.navigator.NavigatorElement;
import lsfusion.server.physics.dev.id.resolve.NamespaceElementFinder.FoundItem;
import lsfusion.server.physics.exec.db.table.ImplementTable;

import java.util.*;

public class DuplicateElementsChecker {
    private Collection<LogicsModule> modules;
    
    public DuplicateElementsChecker(Collection<LogicsModule> modules) {
        this.modules = modules;
    }

    public static class DuplicateElementsFound extends RuntimeException {
        public DuplicateElementsFound(String message) {
            super(message);
        }
    }
    
    public void check() {
        checkForDuplicateElement(new DuplicatePropertyChecker());
        checkForDuplicateElement(new DuplicateActionChecker());
        checkForDuplicateElement(new DuplicateCustomClassChecker());
        checkForDuplicateElement(new DuplicateNavigatorElementChecker());
        checkForDuplicateElement(new DuplicateFormChecker());
        checkForDuplicateElement(new DuplicateTableChecker());
    }

    private <E> void checkForDuplicateElement(Checker<E> helper) {
        Map<String, List<FoundItem<E>>> canonicalNameToElement = new HashMap<>();
        for (LogicsModule module : modules) {
            for (E element : helper.getElements(module)) {
                String cn = helper.getCanonicalName(element);
                if (!canonicalNameToElement.containsKey(cn)) {
                    canonicalNameToElement.put(cn, new ArrayList<FoundItem<E>>());
                }
                canonicalNameToElement.get(cn).add(new FoundItem<>(element, module));
            }
            
            if (hasDuplicateElements(canonicalNameToElement)) {
                String errText = buildDuplicateElementErrorMessage(canonicalNameToElement, helper);
                throw new DuplicateElementsFound(errText);
            }
        }
    }

    private <E> boolean hasDuplicateElements(Map<String, List<E>> canonicalNameToProp) {
        for (List list : canonicalNameToProp.values()) {
            if (list.size() > 1) return true;
        }
        return false;
    }

    private <E> String buildDuplicateElementErrorMessage(Map<String, List<FoundItem<E>>> canonicalNameToProp, Checker<E> helper) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<FoundItem<E>>> entry : canonicalNameToProp.entrySet()) {
            String canonicalName = entry.getKey();
            if (canonicalNameToProp.get(canonicalName).size() > 1) {
                builder.append("\n\t").append("Ambiguous ").append(helper.textName).append(" canonical name '").append(canonicalName).append("': ");
                for (FoundItem<E> item : entry.getValue()) {
                    builder.append("\n\t\tmodule ");
                    builder.append(item.toString());
                }
            }
        }
        return builder.toString();
    }

    private abstract class Checker<T> {
        public abstract Iterable<T> getElements(LogicsModule module);
        public abstract String getCanonicalName(T element);
        public final String textName;

        public Checker(String textName) {
            this.textName = textName;
        }
    }

    private abstract class DuplicatePropertyOrActionChecker<T extends LAP<?, ?>> extends Checker<T> {
        public DuplicatePropertyOrActionChecker(String textName) {
            super(textName);
        }

        @Override
        public String getCanonicalName(T element) {
            return element.property.getCanonicalName();
        }
    }

    private class DuplicatePropertyChecker extends DuplicatePropertyOrActionChecker<LP<?>> {
        public DuplicatePropertyChecker() {
            super("property");
        }

        @Override
        public Iterable<LP<?>> getElements(LogicsModule module) {
            return module.getNamedProperties();
        }
    }

    private class DuplicateActionChecker extends DuplicatePropertyOrActionChecker<LA<?>> {
        public DuplicateActionChecker() {
            super("action");
        }

        @Override
        public Iterable<LA<?>> getElements(LogicsModule module) {
            return module.getNamedActions();
        }
    }

    private class DuplicateCustomClassChecker extends Checker<CustomClass> {
        public DuplicateCustomClassChecker() {
            super("class");
        }

        @Override
        public Iterable<CustomClass> getElements(LogicsModule module) {
            return module.getClasses();
        }

        @Override
        public String getCanonicalName(CustomClass cls) {
            return cls.getCanonicalName();
        }
    }

    private class DuplicateNavigatorElementChecker extends Checker<NavigatorElement> {
        public DuplicateNavigatorElementChecker() {
            super("navigator element");
        }

        @Override
        public Iterable<NavigatorElement> getElements(LogicsModule module) {
            return module.getNavigatorElements();
        }

        @Override
        public String getCanonicalName(NavigatorElement element) {
            return element.getCanonicalName();
        }
    }

    private class DuplicateFormChecker extends Checker<FormEntity> {
        public DuplicateFormChecker() {
            super("form");
        }

        @Override
        public Iterable<FormEntity> getElements(LogicsModule module) {
            return module.getNamedForms();
        }

        @Override
        public String getCanonicalName(FormEntity form) {
            return form.getCanonicalName();
        }
    }

    private class DuplicateTableChecker extends Checker<ImplementTable> {
        public DuplicateTableChecker() {
            super("table");
        }

        @Override
        public Iterable<ImplementTable> getElements(LogicsModule module) {
            return module.getTables();
        }

        @Override
        public String getCanonicalName(ImplementTable table) {
            return table.getCanonicalName();
        }
    }
}

