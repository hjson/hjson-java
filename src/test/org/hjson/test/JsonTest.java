package org.hjson.test;

import org.hjson.JsonValue;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class JsonTest {

    private final Set<String> methodsReported = new HashSet<>();
    protected boolean testsPassing = true;

    abstract void run();

    final boolean allPassing() {
        JsonValue.setEol("\n");
        run();
        return testsPassing;
    }

    protected final void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            System.err.println("Expected:\n" + expected + "\nActual:\n" + actual);
            fail();
        } else {
            pass();
        }
    }

    protected final void assertNotEquals(Object expected, Object actual) {
        if (Objects.equals(expected, actual)) {
            System.err.println("Values should not match:\nExpected:\n" + expected + "\nActual:\n" + actual);
            fail();
        } else {
            pass();
        }
    }

    protected final void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            System.err.println("Expected instance equality:\nExpected:\n" + expected + "\nActual:\n" + actual);
            fail();
        } else {
            pass();
        }
    }

    protected final void assertNotSame(Object expected, Object actual) {
        if (expected == actual) {
            System.err.println("Should be a different instance:\nExpected:\n" + expected + "\nActual:\n" + actual);
            fail();
        } else {
            pass();
        }
    }

    protected final void pass() {
        final String caller = getCaller();
        if (methodsReported.add(caller)) {
            System.out.println("- " + caller + " OK");
        }
    }

    protected final void fail() {
        System.out.println("- " + getCaller() + " FAILED @ " + getCallerDetails());
        this.testsPassing = false;
    }

    private String getCaller() {
        return Thread.currentThread().getStackTrace()[4].getMethodName();
    }

    private String getCallerDetails() {
        final StackTraceElement[] st = Thread.currentThread().getStackTrace();
        return st[3].getMethodName() + "[" + st[4].getLineNumber() + "]";
    }
}
