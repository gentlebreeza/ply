package org.moxie.ply.script;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.moxie.ply.Output;

import java.util.HashMap;
import java.util.Map;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 1:35 PM
 */
public class Junit4RunListener extends RunListener {

    final Map<Description, Failure> failures = new HashMap<Description, Failure>();

    final Map<String, Integer> methodNameOffsets = new HashMap<String, Integer>();

    @Override public void testRunStarted(Description description) throws Exception {
        if (isSyntheticDescription(description)) {
            // ignore...means no-tests or something, process during failure
        } else {
            Output.print("\nRunning tests from ^b^%d^r^ classes\n", description.getChildren().size());
        }
    }

    @Override public void testStarted(Description description) throws Exception {
        if (isSyntheticDescription(description)) {
            return;
        }
        handleNewDescription(description);
        // need to go directly to stdout to avoid Output parsing prior to Exec handling
        System.out.println(String.format("^no_line^\t^b^%s^r^ \t", description.getMethodName()));
    }

    @Override public void testFinished(Description description) throws Exception {
        if (isSyntheticDescription(description)) {
            return;
        }
        // need to go directly to stdout to avoid Output parsing prior to Exec handling
        if (failures.containsKey(description)) {
            Failure failure = failures.get(description);
            String message = createEasilyIdentifiableDiffMessage(failure, failure.getMessage());
            System.out.println(String.format("^no_prefix^^red^^i^ \u2620 FAILURE \u2620 ^r^ %s", message));
        } else {
            System.out.println("^no_prefix^^green^^i^ \u2713 SUCCESS \u2713 ^r^");
        }
    }

    @Override public void testFailure(Failure failure) throws Exception {
        if (isSyntheticDescription(failure.getDescription())) {
            return;
        }
        failures.put(failure.getDescription(), failure);
    }

    @Override public void testIgnored(Description description) throws Exception {
        if (isSyntheticDescription(description)) {
            return;
        }
        handleNewDescription(description);
        System.out.println(String.format("^no_line^\t^b^%s^r^ \t", description.getMethodName()));
        System.out.println("^no_prefix^^yellow^^i^ \u26A0 IGNORED \u26A0 ^r^");
    }

    public static boolean isSyntheticDescription(Description description) {
         // junit reports 'no-tests'/etc as a "test"...filter out.
        return description.getClassName().startsWith("org.junit")
                || description.getClassName().startsWith("junit");
    }

    private void handleNewDescription(Description description) {
        if (!methodNameOffsets.containsKey(description.getClassName())) {
            methodNameOffsets.put(description.getClassName(), 0); // TODO - method name pad
            Output.print("^b^%s^r^", description.getClassName());
        }
    }

    private static String createEasilyIdentifiableDiffMessage(Failure failure, String message) {
        if (message == null) {
            return "";
        }
        // junit already gives nice desc; like: expected:<missis[s]ippi> but was:<missis[]ippi>
        // but add the line number at which the error occurred right after desc.
        Integer lineNumber = null;
        for (StackTraceElement stackTraceElement : failure.getException().getStackTrace()) {
            if (stackTraceElement.getClassName().equals(failure.getDescription().getClassName())) {
                lineNumber = stackTraceElement.getLineNumber();
                break;
            }
        }
        if (lineNumber != null) {
            return String.format("@ ^b^line %d^r^ [ %s ]", lineNumber, message);
        } else {
            return String.format("[ %s ]", message);
        }
    }

}