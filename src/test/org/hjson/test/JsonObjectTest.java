package org.hjson.test;

import org.hjson.JsonObject;
import org.hjson.JsonValue;

import java.util.Arrays;

public class JsonObjectTest extends JsonTest {

    @Override
    void run() {
        getUnusedPaths_returnsUnusedPathsOnly();
        getUsedPaths_returnsUsedPathsOnly();
        getAllPaths_returnsAllPaths();
    }

    void getUnusedPaths_returnsUnusedPathsOnly() {
        final JsonObject subject = parse("a:{b:[{c:{}}]}");
        subject.get("a");

        assertEquals(Arrays.asList("a.b", "a.b[0]", "a.b[0].c"), subject.getUnusedPaths());
    }

    void getUsedPaths_returnsUsedPathsOnly() {
        final JsonObject subject = parse("a:{b:[{c:[]}]}x:[{y:{z:{}}}]");
        subject.get("a").asObject().get("b");
        subject.get("x").asArray().get(0);

        assertEquals(Arrays.asList("a", "a.b", "x", "x[0]"), subject.getUsedPaths());
    }

    void getAllPaths_returnsAllPaths() {

    }

    private static JsonObject parse(final String json) {
        return JsonValue.readHjson(json).asObject();
    }
}
