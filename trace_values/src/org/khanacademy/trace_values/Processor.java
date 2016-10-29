package org.khanacademy.trace_values;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.imports.ImportDeclaration;
import com.github.javaparser.ast.imports.SingleTypeImportDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.utils.Pair;

import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Created by jared on 10/29/16.
 */
public class Processor {
    int id = 0;
    HashMap<Integer, Value> ids = new HashMap<>();
    // Class -> MethodName -> (returnValueId, ids in the fn)
    HashMap<String, HashMap<String, Pair<Integer, List<Integer>>>> instanceMethods = new HashMap<>();
    // Class -> VariableName -> ids of instance vbls
    HashMap<String, HashMap<String, Integer>> instanceVblIds = new HashMap<>();
    // Class -> VariableName -> ids of static vbls
    HashMap<String, HashMap<String, Integer>> staticVblIds = new HashMap<>();

    Processor() {}

    Value.ValueTypeBase fromInitializer(Optional<Expression> init, Type type) {

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
                    ids.put(value.id, value);
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
                handleMethod(methodDeclaration);
                methodDeclaration.getType();
                methodDeclaration.getBody();
                methodDeclaration.getName();
                methodDeclaration.getParameters();
                methodDeclaration.getTypeParameters();
                methodDeclaration.getElementType(); // that's return type I guess
            }
        }
    }

    void handleMethod(MethodDeclaration decl) {
        if (!decl.getBody().isPresent()) {
            return;
        }
        BlockStmt body = decl.getBody().get();
        for (Statement statement : body.getStmts()) {
            
        }
    }
}
