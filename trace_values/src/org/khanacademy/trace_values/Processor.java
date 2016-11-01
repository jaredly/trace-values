package org.khanacademy.trace_values;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.imports.ImportDeclaration;
import com.github.javaparser.ast.imports.SingleTypeImportDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.utils.Pair;

import java.io.Reader;
import java.util.*;

/**
 * Created by jared on 10/29/16.
 */
public class Processor {
    int id = 0;

    static final class Function {
        final List<Integer> arguments;
        final Optional<Integer> returnId;
        final List<Integer> allIds;
        Function(List<Integer> arguments, Optional<Integer> returnId, List<Integer> allIds) {
            this.arguments = arguments;
            this.returnId = returnId;
            this.allIds = allIds;
        }
    }

    HashMap<Integer, Value> ids = new HashMap<>();
    // Class -> MethodName -> (returnValueId, ids in the fn)
    HashMap<String, HashMap<String, Function>> instanceMethods = new HashMap<>();
    // Class -> VariableName -> ids of instance vbls
    HashMap<String, HashMap<String, Integer>> instanceVblIds = new HashMap<>();
    // Class -> VariableName -> ids of static vbls
    HashMap<String, HashMap<String, Integer>> staticVblIds = new HashMap<>();

    Processor() {}

    Value.ValueTypeBase fromInitializer(Optional<Expression> init, Type type) {
        return new Value.Uninitialized(type);
    }

    void process(String path) {

        CompilationUnit file = JavaParser.parse(path);
        processFile(path, file);
    }

    int nextId() {
        this.id += 1;
        return this.id;
    }

    void processFile(String filename, CompilationUnit cu) {
        // TODO maybe use this?
        HashMap<String, String> imports = new HashMap<>();
        for (ImportDeclaration importDeclaration : cu.getImports()) {
            if (importDeclaration instanceof SingleTypeImportDeclaration) {
                String name = ((SingleTypeImportDeclaration) importDeclaration).getType().getName();
                String[] parts = name.split("\\.");
                String last = parts[parts.length - 1];
                imports.put(last, name);
            }
        }

        HashMap<String, Integer> statics = new HashMap<>();
        HashMap<String, Integer> dynamics = new HashMap<>();

        staticVblIds.put(filename, statics);
        instanceVblIds.put(filename, dynamics);

        Path path = Path.fromPackage(cu.getPackage().get());
        // classes
        for (TypeDeclaration<?> typeDeclaration : cu.getTypes()) {
            // instance variables
            for (FieldDeclaration fieldDeclaration : typeDeclaration.getFields()) {
                for (VariableDeclarator variableDeclarator : fieldDeclaration.getVariables()) {
                    String name = variableDeclarator.getId().getName();
                    Value value = new Value(
                            nextId(),
                            fromInitializer(variableDeclarator.getInit(), variableDeclarator.getType()),
                            Pos.fromPosition(filename, variableDeclarator.getBegin()),
                            Pos.fromPosition(filename, variableDeclarator.getEnd()),
                            path.with(name),
                            Optional.of(name)
                    );
                    recordValue(value);
                    if (fieldDeclaration.isStatic()) {
                        statics.put(name, value.id);
                    } else {
                        dynamics.put(name, value.id);
                    }
                }
                // TODO annotations?
                // fieldDeclaration.getAnnotations();
            }

            // instance methods
            // hmmmmm maybe we can lazily expand? yeah. I think so? I guess if we use an instance attribute, we
            // need to evaluate all the methods in the class to see which reference that.
            for (MethodDeclaration methodDeclaration : typeDeclaration.getMethods()) {
                handleMethod(path, filename, methodDeclaration);
                methodDeclaration.getType();
                methodDeclaration.getBody().get().getStmts().get(0)
                methodDeclaration.getName();
                methodDeclaration.getParameters();
                methodDeclaration.getTypeParameters();
                methodDeclaration.getElementType(); // that's return type I guess
            }
        }
    }

    void recordValue(Value value) {
        ids.put(value.id, value);
    }

    private void handleExpression(
            Expression expression,
            Scope scope,
            State state
    ) {
    }

    private final static class State {
        final List<Integer> allIds;
        final List<Integer> returnIds;
        final Path path;
        final String filename;
        State(List<Integer> allIds, List<Integer> returnIds, Path path, String filename) {
            this.allIds=allIds;this.returnIds=returnIds;this.path=path;this.filename=filename;
        }
    }

    public Scope mergeScopes(Scope base, Scope scope1, Scope scope2) {
        HashMap<String, Integer> map = new HashMap<>();
        map.putAll(base.variables);
        for (String key : base.variables.keySet()) {
            int id1 = scope1.get(key);
            int id2 = scope2.get(key);
            if (id1 != id2) {
                Value value = Value.ghost(nextId(), new Value.Join(new Integer[]{id1, id2}));
                recordValue(value);
                map.put(key, value.id);
            }
        }
        return new Scope(map);
    }

    public Scope mergeScopes(Scope base, Scope derived) {
        HashMap<String, Integer> map = new HashMap<>();
        map.putAll(base.variables);
        for (String key : base.variables.keySet()) {
            int id1 = base.get(key);
            int id2 = derived.get(key);
            if (id1 != id2) {
                Value value = Value.ghost(nextId(), new Value.Join(new Integer[]{id1, id2}));
                recordValue(value);
                map.put(key, value.id);
            }
        }
        return new Scope(map);
    }

    private Scope handleStatement(
            Statement statement,
            Scope scope,
            State state
    ) {

        if (statement instanceof IfStmt) {
            IfStmt st = (IfStmt) statement;
            handleExpression(st.getCondition(), scope, state);
            Scope scope1 = handleStatement(st.getThenStmt(), scope.clone(), state);
            if (st.getElseStmt().isPresent()) {
                Scope scope2 = handleStatement(st.getElseStmt().get(), scope.clone(), state);
                return mergeScopes(scope, scope1, scope2);
            } else {
                return mergeScopes(scope, scope1);
            }
        }
        throw new IllegalArgumentException("Don't know how to handle this statement");
    }

    private void handleMethod(Path path, String filename, MethodDeclaration decl) {
        Optional<BlockStmt> maybeBody = decl.getBody();
        if (!maybeBody.isPresent()) {
            return;
        }
        BlockStmt body = maybeBody.get();
        List<Integer> returnIds = new ArrayList<>();
        List<Integer> allIds = new ArrayList<>();

        Scope scope = new Scope();
        // handle arguments
        // this'll be more difficult I think. maybe. buuuut it shouldn't have to be?
        // I just need to have a way of knowing "BTW we're crossing a bridge here" or sth
        // I mean, the
        List<Integer> argumentIds = new ArrayList<>();
        for (Parameter parameter : decl.getParameters()) {
            Value value = new Value(
                    nextId(),
                    new Value.FunctionArgument(parameter.getType()),
                    Pos.fromPosition(filename, parameter.getBegin()),
                    Pos.fromPosition(filename, parameter.getEnd()),
                    path.with(parameter.getName()),
                    Optional.of(parameter.getName())
            );
            recordValue(value);
            scope.set(parameter.getName(), value.id);
            argumentIds.add(value.id);
        }

        State state = new State(allIds, returnIds, path, filename);

        for (Statement statement : body.getStmts()) {
            scope = handleStatement(statement, scope, state);
        }

        final Optional<Integer> returnId;
        if (returnIds.size() == 0) {
            returnId = Optional.empty();
        } else if (returnIds.size() == 1) {
            returnId = Optional.of(returnIds.get(0));
        } else {
            Value returnValue = new Value(
                    nextId(),
                    new Value.Join((Integer[])returnIds.toArray()),
                    Pos.ghost(filename),
                    Pos.ghost(filename),
                    path.with("<return>"),
                    Optional.empty()
            );
            // allIds doesn't contain the returnid.
            // maybe? naw it should.
            allIds.add(returnValue.id);
            recordValue(returnValue);
            returnId = Optional.of(returnValue.id);
        }

        instanceMethods.get(filename).put(decl.getName(), new Function(
                argumentIds,
                returnId,
                allIds
        ));
    }
}
