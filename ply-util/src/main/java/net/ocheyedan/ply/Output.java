package net.ocheyedan.ply;

import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 9:18 PM
 *
 * Defines the output mechanism within the {@literal Ply} application.
 * Support for colored VT-100 terminal output is controlled by the property {@literal color} within the
 * default context, {@literal ply}.
 */
public final class Output {

    /**
     * A regex {@link java.util.regex.Pattern} paired with its corresponding output string.
     */
    private static final class TermCode {
        private final Pattern pattern;
        private final String output;
        private TermCode(Pattern pattern, String output) {
            this.pattern = pattern;
            this.output = output;
        }
    }

    /**
     * Configurable log level variables.
     */
    private static final AtomicBoolean warnLevel = new AtomicBoolean(true);
    private static final AtomicBoolean infoLevel = new AtomicBoolean(true);
    private static final AtomicBoolean dbugLevel = new AtomicBoolean(true);
    /**
     * If true, color will be allowed within output.
     */
    private static final AtomicBoolean coloredOutput = new AtomicBoolean(true);
    /**
     * If false, no ply output will be applied and scripts' output will be printed as-is without any interpretation.
     */
    private static final AtomicBoolean decorated = new AtomicBoolean(true);

    /**
     * A mapping of easily identifiable words to a {@link TermCode} object for colored output.
     */
    private static final Map<String, TermCode> TERM_CODES = new HashMap<String, TermCode>();
    static {
        init();
        String terminal = System.getenv("TERM");
        boolean withinTerminal = (terminal != null);
        // TODO - what are the range of terminal values and what looks best for each?
        String terminalBold = ("xterm".equals(terminal) ? "1" : "0");
        boolean useColor = withinTerminal && coloredOutput.get();
        // first place color values (in case call to Config tries to print, at least have something in
        // TERM_CODES with which to strip messages.
        TERM_CODES.put("ply", new TermCode(Pattern.compile("\\^ply\\^"), useColor ? "[\u001b[0;33mply\u001b[0m]" : "[ply]"));
        TERM_CODES.put("error", new TermCode(Pattern.compile("\\^error\\^"), useColor ? "[\u001b[1;31merr!\u001b[0m]" : "[err!]"));
        TERM_CODES.put("warn", new TermCode(Pattern.compile("\\^warn\\^"), useColor ? "[\u001b[1;33mwarn\u001b[0m]" : "[warn]"));
        TERM_CODES.put("info", new TermCode(Pattern.compile("\\^info\\^"), useColor ? "[\u001b[1;34minfo\u001b[0m]" : "[info]"));
        TERM_CODES.put("dbug", new TermCode(Pattern.compile("\\^dbug\\^"), useColor ? "[\u001b[1;30mdbug\u001b[0m]" : "[dbug]"));
        TERM_CODES.put("reset", new TermCode(Pattern.compile("\\^r\\^"), useColor ? "\u001b[0m" : ""));
        TERM_CODES.put("bold", new TermCode(Pattern.compile("\\^b\\^"), useColor ? "\u001b[1m" : ""));
        TERM_CODES.put("normal", new TermCode(Pattern.compile("\\^n\\^"), useColor ? "\u001b[2m" : ""));
        TERM_CODES.put("inverse", new TermCode(Pattern.compile("\\^i\\^"), useColor ? "\u001b[7m" : ""));
        TERM_CODES.put("black", new TermCode(Pattern.compile("\\^black\\^"), useColor ? "\u001b[" + terminalBold + ";30m" : ""));
        TERM_CODES.put("red", new TermCode(Pattern.compile("\\^red\\^"), useColor ? "\u001b[" + terminalBold + ";31m" : ""));
        TERM_CODES.put("green", new TermCode(Pattern.compile("\\^green\\^"), useColor ? "\u001b[" + terminalBold + ";32m" : ""));
        TERM_CODES.put("yellow", new TermCode(Pattern.compile("\\^yellow\\^"), useColor ? "\u001b[" + terminalBold + ";33m" : ""));
        TERM_CODES.put("blue", new TermCode(Pattern.compile("\\^blue\\^"), useColor ? "\u001b[" + terminalBold + ";34m" : ""));
        TERM_CODES.put("magenta", new TermCode(Pattern.compile("\\^magenta\\^"), useColor ? "\u001b[" + terminalBold + ";35m" : ""));
        TERM_CODES.put("cyan", new TermCode(Pattern.compile("\\^cyan\\^"), useColor ? "\u001b[" + terminalBold + ";36m" : ""));
        TERM_CODES.put("white",
                new TermCode(Pattern.compile("\\^white\\^"), useColor ? "\u001b[" + terminalBold + ";37m" : ""));
    }

    static void init() {
        String logLevels = Props.getValue("log.levels");
        if (!logLevels.contains("warn")) {
            warnLevel.set(false);
        }
        if (!logLevels.contains("info")) {
            infoLevel.set(false);
        }
        if (!logLevels.contains("debug") && !logLevels.contains("dbug")) {
            dbugLevel.set(false);
        }
        String decorated = Props.getValue("decorated");
        if ("false".equalsIgnoreCase(decorated)) {
            Output.decorated.set(false);
        }
        String coloredOutput = Props.getValue("color");
        if ("false".equalsIgnoreCase(coloredOutput)) {
            Output.coloredOutput.set(false);
        }
    }

    public static void print(String message, Object ... args) {
        String formatted = resolve(message, args);
        if ((formatted == null) || (!decorated.get() && isPrintFromPly())) {
            return;
        }
        System.out.println(formatted);
    }

    public static void printNoLine(String message, Object ... args) {
        String formatted = resolve(message, args);
        if ((formatted == null) || (!decorated.get() && isPrintFromPly())) {
            return;
        }
        System.out.print(formatted);
    }
    
    private static boolean isPrintFromPly() {
        // skip ply/ply-util print statements
        StackTraceElement[] stackTrace = new RuntimeException().getStackTrace(); // TODO - better (generic) way?
        if (stackTrace.length > 2) {
            String className = stackTrace[2].getClassName();
            if (className.startsWith("net.ocheyedan.ply") && !className.startsWith("net.ocheyedan.ply.script")) {
                return true;
            }
        }
        return false;
    }

    static void printFromExec(String message, Object ... args) {
        String scriptArg = (String) args[1];
        if (!decorated.get()) {
            System.out.println(scriptArg);
            return;
        }
        boolean noLine = scriptArg.contains("^no_line^");
        boolean noPrefix = scriptArg.contains("^no_prefix^");
        if (noPrefix && noLine) {
            printNoLine("%s", scriptArg.replaceFirst("\\^no_line\\^", "").replaceFirst("\\^no_prefix\\^", ""));
        } else if (noPrefix) {
            print("%s", scriptArg.replaceFirst("\\^no_prefix\\^", ""));
        } else if (noLine) {
            printNoLine(message, args[0], scriptArg.replaceFirst("\\^no_line\\^", ""));
        } else {
            print(message, args);
        }
    }

    public static void print(Throwable t) {
        print("^error^ Message: ^i^^red^%s^r^", (t == null ? "" : t.getMessage()));
    }

    static String resolve(String message, Object[] args) {
        String formatted = String.format(message, args);
        // TODO - fix!  this case fails: ^cyan^warn^r^ if ^warn^ is evaluated first...really meant for ^cyan^ and ^r^
        // TODO - to be resolved
        for (String key : TERM_CODES.keySet()) {
            TermCode termCode = TERM_CODES.get(key);
            Matcher matcher = termCode.pattern.matcher(formatted);
            if (matcher.find()) {
                if (("warn".equals(key) && !warnLevel.get()) || ("info".equals(key) && !infoLevel.get())
                        || ("dbug".equals(key) && !dbugLevel.get())) {
                    // this is a log statement for a disabled log-level, skip.
                    return null;
                }
                if (decorated.get()) {
                    formatted = matcher.replaceAll(termCode.output);
                }
            }
        }
        return formatted;
    }

    /**
     * @return true if warn level logging is enabled
     */
    public static boolean isWarn() {
        return warnLevel.get();
    }

    /**
     * @return true if info level logging is enabled
     */
    public static boolean isInfo() {
        return infoLevel.get();
    }

    /**
     * @return true if debug/dbug level logging is enabled
     */
    public static boolean isDebug() {
        return dbugLevel.get();
    }

    /**
     * @return true if the client can support colored output.
     */
    public static boolean isColoredOutput() {
        return coloredOutput.get();
    }

    /**
     * @return true if ply can print statements and scripts' output should be decorated (i.e., prefixed with script name, etc).
     */
    public static boolean isDecorated() {
        return decorated.get();
    }

    /**
     * Enable warn level logging.
     */
    public static void enableWarn() {
        warnLevel.set(true);
    }

    /**
     * Enables info level logging.
     */
    public static void enableInfo() {
        infoLevel.set(true);
    }

    /**
     * Enables debug/dbug level logging.
     */
    public static void enableDebug() {
        dbugLevel.set(true);
    }

    private Output() { }
}