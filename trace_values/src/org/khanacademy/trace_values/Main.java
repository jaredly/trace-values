package org.khanacademy.trace_values;

import com.github.javaparser.JavaParser;
import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.imports.ImportDeclaration;
import com.github.javaparser.ast.imports.SingleTypeImportDeclaration;
import com.github.javaparser.ast.type.Type;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Plan of action:
 * First
 * - load up all the files (? maybe that'll be memory problem?)
 * - make the global "map of how all things play together"
 *   - <id, {type: Value, start: Pos, end: Pos}>
 *       - Value =
 *          | IntLiteral<int>
 *          | BoolLiteral(bool)
 *          | StringLiteral(string)
 *          | NullLiteral
 *          | ClassLiteral<path, List(args))
 *          | Method<id, name, List<id>, Type>
 *          | StaticMethod<path, List(id), Type)
 *          // | Modified? (maybe)
 * - make a map of <namespaced classname, {attributes: <attr name, }
 */
public class Main {

    public static void main(String[] args) {
	// write your code here
        try {
            String name = "./src/org/khanacademy/trace_values/Main.java"
            new Processor().process(name);
            System.out.print("Hlelo");
        } catch (FileNotFoundException e) {
            // pass
        }
    }
}
