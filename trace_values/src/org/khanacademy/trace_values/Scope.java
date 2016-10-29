package org.khanacademy.trace_values;

import java.util.HashMap;

/**
 * Created by jared on 10/29/16.
 */
public class Scope {
    public final HashMap<String, Integer> variables;

    Scope() {
        variables = new HashMap<>();
    }

    Scope(HashMap<String, Integer> variables) {
        this.variables = variables;
    }

    public Scope clone() {
        HashMap<String, Integer> map = new HashMap<>();
        map.putAll(variables);
        return new Scope(map);
    }

    public void set(String name, int id) {
        variables.put(name, id);
    }

    public int get(String name) {
        return variables.get(name);
    }

}
