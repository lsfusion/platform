package lsfusion.server.logics.property.controller.init;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.controller.init.SimpleBLTask;
import org.apache.log4j.Logger;

import java.util.*;

public class MarkRecursionsTask extends SimpleBLTask {
    @Override
    public String getCaption() {
        return "Looking for recursions in abstract actions";
    }
    
    private final Map<Action<?>, Integer> inIndex = new HashMap<>();
    private final Map<Action<?>, Integer> minIndexReachable = new HashMap<>();
    private final LookupStack stack = new LookupStack();
    private int globalIndex = 0;
    
    @Override
    public void run(Logger logger) {
        tarjanSCCFinder(getBL().getOrderActions());
    }
    
    /**
     * Currently, we assume that recursion can only occur due to abstract actions.
     * Therefore, we need to mark all abstract actions that are part of any dependency cycle.
     * To achieve this, we identify all strongly connected components in the action graph.
     * If an abstract action belongs to a component with more than one node,
     * it indicates that the action is part of a cycle and should be marked accordingly.
     * We will use Tarjan's algorithm for this purpose.
     */
    private void tarjanSCCFinder(ImOrderSet<Action> actions) {
        for (Action<?> action : actions) {
            if (isAbstract(action)) {
                if (!inIndex.containsKey(action)) {
                    tarjanDFS(action);
                }
            }
        }
    }
    
    private void tarjanDFS(Action<?> action) {
        int index = globalIndex;
        int minIndex = globalIndex;
        inIndex.put(action, index);
        
        ++globalIndex;
        
        stack.push(action);
        
        for (Action<?> next : action.getDependActions()) {
            if (!inIndex.containsKey(next)) {
                tarjanDFS(next);
                minIndex = Math.min(minIndex, minIndexReachable.get(next));
            } else if (stack.contains(next)) {
                minIndex = Math.min(minIndex, inIndex.get(next));
            }
        }
        
        minIndexReachable.put(action, minIndex);
        boolean isComponentFound = index == minIndex;
        if (isComponentFound) {
            boolean componentHasCycle = stack.peek() != action;
            popComponentFromStack(action, componentHasCycle);
        }
    }
    
    private void popComponentFromStack(Action<?> action, boolean componentHasCycle) {
        while (!stack.isEmpty()) {
            Action<?> last = stack.pop();
            if (componentHasCycle) {
                markRecursiveIfNeeded(last);
            }
            if (last == action) {
                break;
            }
        }
    }
    
    private boolean isAbstract(Action<?> action) {
        return action instanceof ListCaseAction && ((ListCaseAction) action).isAbstract();
    }
    
    private void markRecursiveIfNeeded(Action<?> action) {
        if (isAbstract(action)) {
            ((ListCaseAction) action).setRecursive(true);
        }
    }
    
    private static class LookupStack {
        private final Set<Action<?>> inStack = new HashSet<>();
        private final ArrayList<Action<?>> stack = new ArrayList<>();
        
        public void push(Action<?> action) {
            inStack.add(action);
            stack.add(action);
        }
        
        public boolean isEmpty() {
            return stack.isEmpty();
        }
        
        public Action<?> pop() {
            assert !isEmpty();
            inStack.remove(stack.get(stack.size() - 1));
            return stack.remove(stack.size() - 1);
        }
        
        public Action<?> peek() {
            assert !isEmpty();
            return stack.get(stack.size() - 1);
        }
        
        public boolean contains(Action<?> action) {
            return inStack.contains(action);
        }
    }
}
