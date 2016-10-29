package org.khanacademy.trace_values;

import com.github.javaparser.ast.type.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Created by jared on 10/29/16.
 */
public class Value {
    final int id;
    final ValueTypeBase type;
    final Optional<String> name;
    final Pos start;
    final Pos end;
    final Path path;
    final List<Integer> children;
    public Value(int id, ValueTypeBase type, Pos start, Pos end, Path path, Optional<String> name) {
        this.id = id;
        this.type = type;
        this.start = start;
        this.end = end;
        this.path = path;
        this.name = name;
        this.children = new ArrayList<>();
    }

    void addChild(int child) {
        this.children.add(child);
    }

    public static class ValueTypeBase { }

    public static final class IntLiteral extends ValueTypeBase {
        final int value;
        IntLiteral(int value) {
            this.value = value;
        }
    }

    public static final class StringLiteral extends ValueTypeBase {
        final String value;
        StringLiteral(String value) {
            this.value = value;
        }
    }

    public static final class ClassLiteral extends ValueTypeBase {
        final int id;
        final int[] args;
        final Type type;
        ClassLiteral(int id, int[] args, Type type) {
            this.id = id;
            this.args = args;
            this.type = type;
        }
    }

    public static final class StaticMethod extends ValueTypeBase {
        final String path;
        final int[] args;
        final Type type;
        StaticMethod(String path, int[] args, Type type) {
            this.path = path;
            this.args = args;
            this.type = type;
        }
    }

    public static final class BinOp extends ValueTypeBase {
        final int left; final int right; final String op;
        BinOp(int left, int right, String op){
            this.left=left;this.right=right;this.op=op;
        }
    }

    /**
     * After an `if`, for example, if the two branches have different values for a variable,
     * this will join the two possibilities together.
     */
    public static final class Join extends ValueTypeBase {
        final int first; final int second;
        Join(int first, int second) {
            this.first=first;this.second=second;
        }

    }

    public static final class Method extends ValueTypeBase {
        final int id; final int[] args; final Type type;
        Method(int id, int[] args, Type type) {
            this.id = id; this.args = args; this.type = type;
        }
    }

    public static final class Uninitialized extends ValueTypeBase {
        final Type type;
        Uninitialized(Type type) {
            this.type = type;
        }
    }

    public static final class BoolLiteral extends ValueTypeBase {
        final boolean value;
        BoolLiteral(boolean value) {
            this.value = value;
        }
    }
}
