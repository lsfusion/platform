package lsfusion.server.physics.dev.module;

import lsfusion.server.logics.LogicsModule;

import java.util.*;

import static lsfusion.base.BaseUtils.isRedundantString;

public class ModuleList {
    private static final String[] systemModulesNames = {"System", "Authentication", "Email", "Reflection",
            "Scheduler", "Security", "Service", "SystemEvents", "Time", "Utils"};
    
    private List<LogicsModule> modules = new ArrayList<>();

    public void add(LogicsModule module) {
        modules.add(module);
    }
    
    
    public LogicsModule get(String name) {
        return nameToModule.get(name);
    }

    public List<LogicsModule> all() {
        return modules;
    } 
    
    private Map<String, LogicsModule> nameToModule = new HashMap<>();
    private String orderDependencies;
    
    public void setOrderDependencies(String orderDependencies) {
        this.orderDependencies = orderDependencies;
    }
    
    public void orderModules() {
//        outDotFile();
        Map<String, List<String>> graph = buildModuleGraphWithOrderDependencies();
        modules = dfsTopologicalSort(graph);
    }
    
    private Map<String, List<String>> buildModuleGraphWithOrderDependencies() {
        Map<String, List<String>> graph = buildModuleGraph();

        checkCycles(graph, "there is a circular dependency between required modules");

        if (!isRedundantString(orderDependencies)) {
            addOrderDependencies(orderDependencies, graph);
            checkCycles(graph, "there is a circular dependency introduced by order dependencies");
        }
        return graph;
    }
    
    public void filterWithTopModule(String topModuleName) {
        Set<LogicsModule> remainingModules = new HashSet<>();
        Queue<LogicsModule> queue = new LinkedList<>();

        // First, we always add system modules   
        for (String systemModuleName : systemModulesNames) {
            remainingModules.add(getModuleWithCheck(systemModuleName));
        }

        LogicsModule startModule = getModuleWithCheck(topModuleName);
        queue.add(startModule);
        remainingModules.add(startModule);

        while (!queue.isEmpty()) {
            LogicsModule current = queue.poll();

            assert current != null;
            for (String nextModuleName : current.getRequiredNames()) {
                LogicsModule nextModule = getRequiredModuleWithCheck(nextModuleName, current.getName());
                if (!remainingModules.contains(nextModule)) {
                    remainingModules.add(nextModule);
                    queue.add(nextModule);
                }
            }
        }
        resetModules(remainingModules);
    }

    private void resetModules(Collection<LogicsModule> newModules) {
        modules.clear();
        for (LogicsModule module : newModules) {
            add(module);
        }
        fillNameToModules();
    }
    
    private LogicsModule getModuleWithCheck(String moduleName) {
        LogicsModule module = get(moduleName);
        if (module == null) {
            throw new RuntimeException(String.format("Module '%s' not found.", moduleName));
        }
        return module;
    }

    private LogicsModule getRequiredModuleWithCheck(String moduleName, String parentModuleName) {
        LogicsModule module = get(moduleName);
        if (module == null) {
            throw new RuntimeException(String.format("Error in module '%s': required module '%s' was not found.", parentModuleName, moduleName));
        }
        return module;
    }
    
    private List<LogicsModule> dfsTopologicalSort(Map<String, List<String>> graph) {
        List<LogicsModule> rootModules = getRootModules(graph);

        Set<String> visited = new LinkedHashSet<>();
        List<LogicsModule> modules = new ArrayList<>();
        for (LogicsModule rootModule : rootModules) {
            dfsModules(rootModule.getName(), graph, visited, modules);
        }
        return modules;
    }

    private void dfsModules(String curModuleName, Map<String, List<String>> graph, Set<String> visited, List<LogicsModule> out) {
        visited.add(curModuleName);
        for (String nextModuleName : graph.get(curModuleName)) {
            if (!visited.contains(nextModuleName)) {
                dfsModules(nextModuleName, graph, visited, out);
            }
        }
        out.add(nameToModule.get(curModuleName));
    }

    private List<LogicsModule> getRootModules(Map<String, List<String>> graph) {
        Set<String> rootModuleNames = new HashSet<>(graph.keySet());
        for (String moduleName : graph.keySet()) {
            for (String reqModuleName : graph.get(moduleName)) {
                rootModuleNames.remove(reqModuleName);
            }
        }
        
        List<LogicsModule> rootModules = new ArrayList<>();
        for (String moduleName : rootModuleNames) {
            rootModules.add(nameToModule.get(moduleName));
        }
        return rootModules; 
    }  
    
    private static void checkCycles(Map<String, List<String>> graph, String errorMessage) {
        Set<String> used = new HashSet<>();
        for (String vertex : graph.keySet()) {
            if (!used.contains(vertex)) {
                String foundCycle = checkCycles(vertex, new LinkedList<String>(), used, graph);
                if (foundCycle != null) {
                    throw new RuntimeException("[error]:\t" + errorMessage + ": " + foundCycle);
                }
            }
        }
    }

    private static String checkCycles(String cur, LinkedList<String> way, Set<String> used, Map<String, List<String>> graph) {
        way.add(cur);
        used.add(cur);
        for (String next : graph.get(cur)) {
            if (!used.contains(next)) {
                String foundCycle = checkCycles(next, way, used, graph);
                if (foundCycle != null) {
                    return foundCycle;
                }
            } else if (way.contains(next)) {
                StringBuilder foundCycle = new StringBuilder(next);
                do {
                    foundCycle.append(" <- ").append(way.peekLast());
                } while (!Objects.equals(way.pollLast(), next));
                return foundCycle.toString();
            }
        }

        way.removeLast();
        return null;
    }

//    // Creates .dot file (grafviz) with the module hierarchy graph  
//    private void outDotFile() {
//        try {
//            FileWriter fStream = new FileWriter("D:/lsf/modules.dot");
//            BufferedWriter out = new BufferedWriter(fStream);
//            out.write("digraph Modules {\n");
//            out.write("\tsize=\"6,4\"; ratio = fill;\n");
//            out.write("\tnode [shape=box, fontsize=60, style=filled];\n");
//            out.write("\tedge [arrowsize=2];\n");
//            for (LogicsModule module : modules) {
//                for (String name : module.getRequiredNames()) {
//                    out.write("\t" + name + " -> " + module.getName() + ";\n");
//                }
//            }
//            out.write("}\n");
//            out.close();
//        } catch (Exception e) {
//        }
//    }

    private void addOrderDependencies(String orderDependencies, Map<String, List<String>> graph) {
        for (String dependencyList : orderDependencies.split(";\\s*")) {
            String dependencies[] = dependencyList.split(",\\s*");
            for (int i = 0; i < dependencies.length; ++i) {
                String moduleName2 = dependencies[i];
                if (graph.get(moduleName2) == null) {
                    throw new RuntimeException(String.format("[error]:\torder dependencies' module '%s' was not found", moduleName2));
                }

                if (i > 0) {
                    String moduleName1 = dependencies[i - 1];
                    graph.get(moduleName1).add(moduleName2);
                }
            }
        }
    }

    public Map<String, List<String>> buildModuleGraph() {
        Map<String, List<String>> graph = new HashMap<>();
        for (LogicsModule module : modules) {
            graph.put(module.getName(), new ArrayList<String>());
        }

        for (LogicsModule module : modules) {
            List<String> moduleNeighbours = graph.get(module.getName());
            for (String reqModuleName : module.getRequiredNames()) {
                if (graph.get(reqModuleName) == null) {
                    throw new RuntimeException(String.format("[error]:\t%s:\trequired module '%s' was not found", module.getName(), reqModuleName));
                }
                moduleNeighbours.add(reqModuleName);
            }
        }
        return graph;
    }

    public void fillNameToModules() {
        nameToModule.clear();
        for (LogicsModule module : modules) {
            if (nameToModule.containsKey(module.getName())) {
                throw new RuntimeException(String.format("[error]:\tmodule '%s' has already been added", module.getName()));
            }
            nameToModule.put(module.getName(), module);
        }
    }

}
