package org.jrdf.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility which applies N-Triples escaping.
 *
 * @author Andrew Newman
 * @version $Revision: 624 $
 */
public class EscapeUtil {
    /**
     * A regular expression to pick out characters needing escape from Unicode to
     * ASCII.  A different regular expression is used depending on which version of the JDK is detected - Java 1.4 has
     * different character support compared with 1.5 and above.
     * <p/>
     * This is used by the {@link #escape} method.
     */
    private static Pattern pattern;

    static {
        try {
            if (System.getProperty("java.version").indexOf("1.4") >= 0) {
                pattern = Pattern.compile("[\\uD800-\\uDBFF][\\uDC00-\\uDFFF]" +
                        "|" +
                        "[\\x00-\\x1F\\x22\\\\\\x7F-\\uFFFF]");
            } else {
                pattern = Pattern.compile("[\uD800\uDC00-\uDBFF\uDFFF]" +
                        "|" +
                        "[\\x00-\\x1F\\x22\\\\\\x7F-\\uFFFF]");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Base UTF Code point.
     */
    private static final int UTF_BASE_CODEPOINT = 0x10000;

    /**
     * How shift to get UTF-16 to character codes.
     */
    private static final int CHARACTER_CODE_OFFSET = 0x3FF;

    /**
     * How many characters at a time to decode for 8 bit encoding.
     */
    private static final int CHARACTER_LENGTH_8_BIT = 11;

    /**
     * How many characters at a time to decode for 16 bit encoding.
     */
    private static final int CHARACTER_LENGTH_16_BIT = 7;

    private EscapeUtil() {
    }

    /**
     * Escapes a string literal to a string that is N-Triple escaped.
     *
     * @param string a string to escape, never <code>null</code>.
     * @return a version of the <var>string</var> with N-Triples escapes applied.
     */
    public static String escape(String string) {
        assert null != string;

        // Obtain a fresh matcher
        Matcher matcher = pattern.matcher(string);

        // Try to short-circuit the whole process -- maybe nothing needs escaping?
        if (!matcher.find()) {
            return string;
        }

        // Perform escape character substitutions on each match found by the
        // matcher, accumulating the escaped text into a stringBuffer
        StringBuffer stringBuffer = new StringBuffer();
        do {
            // The escape text with which to replace the current match
            String escapeString;

            // Depending of the character sequence we're escaping, determine an
            // appropriate replacement
            String groupString = matcher.group();
            switch (groupString.length()) {
                case 1: // 16-bit characters requiring escaping
                    switch (groupString.charAt(0)) {
                        case '\t': // tab
                            escapeString = "\\\\t";
                            break;
                        case '\n': // newline
                            escapeString = "\\\\n";
                            break;
                        case '\r': // carriage return
                            escapeString = "\\\\r";
                            break;
                        case '"':  // quote
                            escapeString = "\\\\\\\"";
                            break;
                        case '\\': // backslash
                            escapeString = "\\\\\\\\";
                            break;
                        default:   // other characters use 4-digit hex escapes
                            String hexString = Integer.toHexString(groupString.charAt(0)).toUpperCase();

                            escapeString = "\\\\u0000".substring(0, CHARACTER_LENGTH_16_BIT - hexString.length()) +
                                    hexString;

                            assert CHARACTER_LENGTH_16_BIT == escapeString.length();
                            assert escapeString.startsWith("\\\\u");
                            break;
                    }
                    break;

                case 2: // surrogate pairs are represented as 8-digit hex escapes
                    assert Character.SURROGATE == Character.getType(groupString.charAt(0));
                    assert Character.SURROGATE == Character.getType(groupString.charAt(1));

                    int highSurrogate = ((groupString.charAt(0) & CHARACTER_CODE_OFFSET) << 10);
                    int lowSurrogate = (groupString.charAt(1) & CHARACTER_CODE_OFFSET);
                    String hexString = Integer.toHexString(highSurrogate + lowSurrogate + UTF_BASE_CODEPOINT).
                            toUpperCase();
                    escapeString = "\\\\U00000000".substring(0, CHARACTER_LENGTH_8_BIT - hexString.length()) +
                            hexString;

                    assert CHARACTER_LENGTH_8_BIT == escapeString.length();
                    assert escapeString.startsWith("\\\\U00") : "Expected a start of \\\\U00, but got " + escapeString;
                    break;

                default:
                    throw new Error("Escape sequence " + groupString + " has no handler");
            }
            assert null != escapeString;

            // Having determined an appropriate escapeString, add it to the
            // stringBuffer
            matcher.appendReplacement(stringBuffer, escapeString);
        }
        while (matcher.find());

        // Finish off by appending any remaining text that didn't require escaping,
        // and return the assembled buffer
        matcher.appendTail(stringBuffer);
        return stringBuffer.toString();
    }
}
