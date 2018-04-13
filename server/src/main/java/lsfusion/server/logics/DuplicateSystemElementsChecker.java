package lsfusion.server.logics;

import lsfusion.server.classes.CustomClass;
import lsfusion.server.form.entity.FormEntity;
import lsfusion.server.form.navigator.NavigatorElement;
import lsfusion.server.logics.debug.DebugInfo;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.table.ImplementTable;

import java.util.*;

public class DuplicateSystemElementsChecker {
    private Collection<LogicsModule> modules;
    
    public DuplicateSystemElementsChecker(Collection<LogicsModule> modules) {
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

    private <E> void checkForDuplicateElement(DuplicateElementsChecker<E> helper) {
        Map<String, List<FoundItem<E>>> canonicalNameToElement = new HashMap<>();
        for (LogicsModule module : modules) {
            for (E element : helper.getElements(module)) {
                String cn = helper.getCanonicalName(element);
                if (!canonicalNameToElement.containsKey(cn)) {
                    canonicalNameToElement.put(cn, new ArrayList<FoundItem<E>>());
                }
                canonicalNameToElement.get(cn).add(new FoundItem<>(element, module.getName()));
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

    private static class FoundItem<E> {
        public final String moduleName;
        public final E element;

        FoundItem(E element, String moduleName) {
            this.element = element;
            this.moduleName = moduleName;
        }
    }

    private <E> String buildDuplicateElementErrorMessage(Map<String, List<FoundItem<E>>> canonicalNameToProp, DuplicateElementsChecker<E> helper) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, List<FoundItem<E>>> entry : canonicalNameToProp.entrySet()) {
            String canonicalName = entry.getKey();
            if (canonicalNameToProp.get(canonicalName).size() > 1) {
                builder.append("\n\t").append("Ambiguous ").append(helper.textName).append(" canonical name '").append(canonicalName).append("': ");
                for (FoundItem<E> item : entry.getValue()) {
                    builder.append("\n\t\tmodule ");
                    DebugInfo.DebugPoint point = helper.getDebugPoint(item.element);
                    if (point != null) {
                        builder.append(point.toString());
                    } else {
                        builder.append(item.moduleName);
                    }
                }
            }
        }
        return builder.toString();
    }

    private abstract class DuplicateElementsChecker<T> {
        public abstract DebugInfo.DebugPoint getDebugPoint(T element);
        public abstract Iterable<T> getElements(LogicsModule module);
        public abstract String getCanonicalName(T element);
        public final String textName;

        public DuplicateElementsChecker(String textName) {
            this.textName = textName;
        }
    }

    private abstract class DuplicatePropertyOrActionChecker<T extends LP<?, ?>> extends DuplicateElementsChecker<T> {
        public DuplicatePropertyOrActionChecker(String textName) {
            super(textName);
        }

        @Override
        public DebugInfo.DebugPoint getDebugPoint(T element) {
            if (element.property.getDebugInfo() == null) {
                return null;
            } else {
                return element.property.getDebugInfo().getPoint();
            }
        }

        @Override
        public String getCanonicalName(T element) {
            return element.property.getCanonicalName();
        }
    }

    private class DuplicatePropertyChecker extends DuplicatePropertyOrActionChecker<LCP<?>> {
        public DuplicatePropertyChecker() {
            super("property");
        }

        @Override
        public Iterable<LCP<?>> getElements(LogicsModule module) {
            return module.getNamedProperties();
        }
    }

    private class DuplicateActionChecker extends DuplicatePropertyOrActionChecker<LAP<?>> {
        public DuplicateActionChecker() {
            super("action");
        }

        @Override
        public Iterable<LAP<?>> getElements(LogicsModule module) {
            return module.getNamedActions();
        }
    }

    private class DuplicateCustomClassChecker extends DuplicateElementsChecker<CustomClass> {
        public DuplicateCustomClassChecker() {
            super("class");
        }

        @Override
        public DebugInfo.DebugPoint getDebugPoint(CustomClass cls) {
            return cls.getDebugInfo().getPoint();
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

    private class DuplicateNavigatorElementChecker extends DuplicateElementsChecker<NavigatorElement> {
        public DuplicateNavigatorElementChecker() {
            super("navigator element");
        }

        @Override
        public DebugInfo.DebugPoint getDebugPoint(NavigatorElement element) {
            return element.getDebugPoint();
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

    private class DuplicateFormChecker extends DuplicateElementsChecker<FormEntity> {
        public DuplicateFormChecker() {
            super("form");
        }

        @Override
        public DebugInfo.DebugPoint getDebugPoint(FormEntity form) {
            return form.getDebugPoint();
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

    private class DuplicateTableChecker extends DuplicateElementsChecker<ImplementTable> {
        public DuplicateTableChecker() {
            super("table");
        }

        @Override
        public DebugInfo.DebugPoint getDebugPoint(ImplementTable table) {
            return null;
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

