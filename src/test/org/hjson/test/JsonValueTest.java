package org.hjson.test;

import org.hjson.CommentStyle;
import org.hjson.JsonLiteral;
import org.hjson.JsonValue;

import java.util.Objects;

final class JsonValueTest {

    private boolean testsPassing = true;

    boolean allPassing() {
        JsonValue.setEol("\n");
        formatComment_generatesHashComment();
        formatComment_generatesLineComment();
        formatComment_generatesBlockComment();
        formatComment_generatesMultilineHash();
        formatComment_generatesMultilineLine();
        stripComment_stripsHashComment();
        stripComment_stripsLineComment();
        stripComment_stripsBlockComment();
        stripComment_stripsMultilineHash();
        stripComment_stripsMultilineLine();
        stripComment_stripsMultilineBlock();
        stripComment_stripsComplexComment();
        setComment_getCommentText_preservesExactText();
        return testsPassing;
    }

    private void formatComment_generatesHashComment() {
        final String comment = "here's a comment";
        final String expected = "# here's a comment";

        assertEquals(expected, JsonValue.formatComment(CommentStyle.HASH, comment));
    }

    private void formatComment_generatesLineComment() {
        final String comment = "here's another comment";
        final String expected = "// here's another comment";

        assertEquals(expected, JsonValue.formatComment(CommentStyle.LINE, comment));
    }

    private void formatComment_generatesBlockComment() {
        final String comment = "here's a block comment";
        final String expected = "/*\nhere's a block comment\n*/";

        assertEquals(expected, JsonValue.formatComment(CommentStyle.BLOCK, comment));
    }

    private void formatComment_generatesMultilineHash() {
        final String comment = "here's a comment\nwith multiple lines";
        final String expected = "# here's a comment\n# with multiple lines";

        assertEquals(expected, JsonValue.formatComment(CommentStyle.HASH, comment));
    }

    private void formatComment_generatesMultilineLine() {
        final String comment = "here's a comment\nwith multiple lines";
        final String expected = "// here's a comment\n// with multiple lines";

        assertEquals(expected, JsonValue.formatComment(CommentStyle.LINE, comment));
    }

    private void stripComment_stripsHashComment() {
        final String comment = "# hashed comment";
        final String expected = "hashed comment";

        assertEquals(expected, JsonValue.stripComment(comment));
    }

    private void stripComment_stripsLineComment() {
        final String comment = "// line comment";
        final String expected = "line comment";

        assertEquals(expected, JsonValue.stripComment(comment));
    }

    private void stripComment_stripsBlockComment() {
        final String comment = "/* block comment */";
        final String expected = "block comment";

        assertEquals(expected, JsonValue.stripComment(comment));
    }

    private void stripComment_stripsMultilineHash() {
        final String comment = "# hashed comment\n# second line";
        final String expected = "hashed comment\nsecond line";

        assertEquals(expected, JsonValue.stripComment(comment));
    }

    private void stripComment_stripsMultilineLine() {
        final String comment = "// line comment\n// second line";
        final String expected = "line comment\nsecond line";

        assertEquals(expected, JsonValue.stripComment(comment));
    }

    private void stripComment_stripsMultilineBlock() {
        final String comment = "/* block comment\n* second line\n */";
        final String expected = "block comment\nsecond line";

        assertEquals(expected, JsonValue.stripComment(comment));
    }

    private void stripComment_stripsComplexComment() {
        final String comment = "/* block comment\n* second line\n /*\n# third line\n// fourth line";
        final String expected = "block comment\nsecond line\nthird line\nfourth line";

        assertEquals(expected, JsonValue.stripComment(comment));
    }

    private void setComment_getCommentText_preservesExactText() {
        final String comment = "Hello, World!";
        final JsonValue value = JsonLiteral.jsonNull().setComment(comment);

        assertEquals(comment, value.getCommentText());
    }

    private void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            System.err.println("Expected:\n" + expected + "\nActual:\n" + actual);
            fail(1);
        }
        pass(1);
    }

    private void pass() {
        pass(0);
    }

    private void fail() {
        fail(0);
    }

    private void pass(int up) {
        System.out.println("- " + getCaller(up) + " OK");
    }

    private void fail(int up) {
        System.err.println("- " + getCaller(up) + " FAILED");
        this.testsPassing = false;
    }

    private String getCaller(int up) {
        return Thread.currentThread().getStackTrace()[3 + up].getMethodName();
    }
}
