package org.khanacademy.trace_values;

import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.expr.QualifiedNameExpr;

import java.util.Arrays;
import java.util.List;

/**
 * Created by jared on 10/29/16.
 */
public class Path {
    List<String> items;
    Path(List<String> items) {
        this.items = items;
    }

    static Path fromPackage(PackageDeclaration pack) {
        return new Path(Arrays.asList(new String[]{pack.getName().getQualifiedName()}));
    }

    @Override
    public String toString() {
        return String.join(".", items);
    }

    Path with(String item) {
        List<String> items = Arrays.asList((String[])this.items.toArray());
        items.add(item);
        return new Path(items);
    }
}
