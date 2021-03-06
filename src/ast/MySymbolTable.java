package ast;

import ast.Method;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MySymbolTable implements SymbolTable {

    private final List<Symbol> fields;
    private final List<String> imports, methodNames;
    private final Map<String, Method> methods;
    private String superClass = null;
    private String className = null;

    private final Map<String, Map<String, String>> overloadingMap;

    public MySymbolTable() {
        this.fields = new ArrayList<>();
        this.imports = new ArrayList<>();
        this.methodNames = new ArrayList<>();
        this.methods = new HashMap<>();
        this.overloadingMap = new HashMap<>();
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public boolean addField(Symbol symbol) {
        for (Symbol s : fields)
            if (symbol.getName().equals(s.getName()))
                return false;
        this.fields.add(symbol);
        return true;
    }

    public void addImport(String importName) {
        this.imports.add(importName);
    }

    /**
     *
     * @param method
     * @return True upon success, false otherwise
     */
    public boolean addMethod(Method method) {
        if (method.getName().equals("main")) {
            if (this.methodNames.contains(("main")))
                return false;

            this.methodNames.add("main");
            this.methods.put("main", method);
            return true;
        }

        Map<String, String> methodEntry = this.overloadingMap.computeIfAbsent(method.getName(), k -> new HashMap<>());
        String typesStr = generateParamsString(method.getParamameterTypes());
        if (methodEntry.containsKey(typesStr))
            return false;
        String nameId = method.getName() + methodEntry.size();
        method.setUniqueName(nameId);
        methodEntry.put(typesStr, nameId);
        this.methodNames.add(nameId);
        this.methods.put(nameId, method);

        return true;
    }

    public Method getMethod(String name) {
        return name == null ? null : this.methods.get(name);
    }

    private String generateParamsString(List<Type> parameters) {
        StringBuilder mapName = new StringBuilder();
        boolean first = true;
        for (Type t: parameters) {
            if (t == null)
                throw new RuntimeException("Received unexpected null Type");

            if (!first) mapName.append("-");
            else first = false;

            if (t.getName().equals("-ignore"))
                mapName.append("?");
            else
                mapName.append(t.getName()).append(t.isArray() ? "[]" : "");
        }
        return mapName.toString();
    }

    private String getCompatibleMethod(Map<String, String> overloads, String paramString, String methodName) {
        String[] params = paramString.split("-");

        String found = null;
        for (String s: overloads.keySet()) {
            String[] ovrld = s.split("-");
            if (ovrld.length != params.length)
                continue;

            boolean diff = false;
            for (int i = 0; i < ovrld.length; i++) {
                if (!params[i].equals("?") && !params[i].equals(ovrld[i])) {
                    diff = true;
                    break;
                }
            }

            if (!diff) {
                if (found == null)
                    found = overloads.get(s);
                else
                    throw new IndistinguishableMethod("Multiple " + methodName + " methods found with a signature matching " + paramString);
            }
        }

        return found;
    }

    public String getUniqueName(String name, List<Type> parameters) {
        if (name.equals("main"))
            return "main";

        Map<String, String> overloads = this.overloadingMap.get(name);
        if (overloads == null)
            return null;

        String paramString = generateParamsString(parameters);
        if (paramString.contains("?")) {
            // Deduce type
            return getCompatibleMethod(overloads, paramString, name);
        } else {
            // Simple lookup
            return overloads.get(paramString);
        }
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return methodNames;
    }

    @Override
    public Type getReturnType(String methodName) {
        Method m = getMethod(methodName);
        return m == null ? null : m.getReturnType();
    }

    @Override
    public List<Symbol> getParameters(String methodName) {
        Method m = getMethod(methodName);
        return m == null ? null : m.getParameters();
    }

    @Override
    public List<Symbol> getLocalVariables(String methodName) {
        Method m = getMethod(methodName);
        return m == null ? null : m.getLocalVariables();
    }

    @Override
    public String toString() {
        StringBuilder fieldsString = new StringBuilder();
        for (Symbol s : fields) {
            fieldsString.append("\t").append(s).append("\n");
        }
        StringBuilder methodsString = new StringBuilder();
        for (Method s : methods.values()) {
            methodsString.append("\t").append(s).append("\n");
        }
        return  "className='" + className + "'\n" +
                "superClass='" + superClass + "'\n" +
                "imports=" + imports + "\n" +
                "fields=[\n" + fieldsString + "]\n" +
                "methods=[\n" + methodsString +
                "]\n}";
    }
}
