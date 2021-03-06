package net.ocheyedan.ply.script.print;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * User: blangel
 * Date: 10/29/11
 * Time: 2:35 PM
 *
 * All calls go to a file.  Calls to {@link #print(String)} and {@link #println(String)} go to a file unless the String
 * starts with {@link #PRIVILEGED_PREFIX} in which case the call is delegated to {@link #delegate}.
 */
public class PrivilegedPrintStream extends PrintStream {

    public static final String PRIVILEGED_PREFIX = "^priv^";

    private final PrintStream delegate;

    public PrivilegedPrintStream(PrintStream delegate, File file) throws FileNotFoundException, UnsupportedEncodingException {
        super(file);
        this.delegate = new PrintStream(delegate, true, "UTF-8");
    }

    @Override public void print(String out) {
        if ((out != null) && out.startsWith(PRIVILEGED_PREFIX)) {
            this.delegate.print(out.substring(PRIVILEGED_PREFIX.length()));
        } else {
            super.print(out);
        }
    }

    @Override public void println(String out) {
        if ((out != null) && out.startsWith(PRIVILEGED_PREFIX)) {
            this.delegate.println(out.substring(PRIVILEGED_PREFIX.length()));
        } else {
            super.println(out);
        }
    }
}
