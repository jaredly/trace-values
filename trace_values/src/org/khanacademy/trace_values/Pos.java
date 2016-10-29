package org.khanacademy.trace_values;

import com.github.javaparser.Position;

/**
 * Created by jared on 10/29/16.
 */
public class Pos {
    public final String path;
    public final int line;
    public final int col;
    public Pos(String path, int line, int col) {
        this.path = path;
        this.line = line;
        this.col = col;
    }

    public static Pos ghost(String path) {
        return new Pos(path, -1, -1);
    }

    public static Pos fromPosition(String path, Position pos) {
        return new Pos(path, pos.line, pos.column);
    }
}
