/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.util;

import java.util.Formatter;
import java.util.FormattableFlags;
import org.apache.sis.util.Static;
import org.apache.sis.util.Classes;
import org.apache.sis.util.Characters;
import org.apache.sis.util.CharSequences;


/**
 * Miscellaneous utilities which should not be put in public API.
 * Most of those methods are for {@link Object#toString()} implementations.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.3
 * @module
 */
public final class Strings extends Static {
    /**
     * The character to write at the beginning of lines that are continuation of a single log record.
     * This constant is defined here only for a little bit more uniform {@code toString()} in SIS.
     */
    public static final char CONTINUATION_MARK = '┃', CONTINUATION_END = '╹';

    /**
     * Characters for a new item in a block illustrated by {@link #CONTINUATION_MARK}.
     * This constant is defined here only for a little bit more uniform {@code toString()} in SIS.
     */
    public static final String CONTINUATION_ITEM = "▶ ";

    /**
     * Do not allow instantiation of this class.
     */
    private Strings() {
    }

    /**
     * Appends to the given buffer only the characters that are valid for a Unicode identifier.
     * The given separator character is append before the given {@code text} only if the buffer
     * is not empty and at least one {@code text} character is valid.
     *
     * <div class="section">Relationship with {@code gml:id}</div>
     * This method may be invoked for building {@code gml:id} values. Strictly speaking this is not appropriate
     * since the {@code xs:ID} type defines valid identifiers as containing only letters, digits, underscores,
     * hyphens, and periods. This differ from Unicode identifier in two ways:
     *
     * <ul>
     *   <li>Unicode identifiers accept Japanese or Chinese ideograms for instance, which are considered as letters.</li>
     *   <li>Unicode identifiers do not accept the {@code '-'} and {@code ':'} characters. However this restriction
     *       fits well our need, since those characters are typical values for the {@code separator} argument.</li>
     *   <li>Note that {@code '_'} is valid both in {@code xs:ID} and Unicode identifier.</li>
     * </ul>
     *
     * @param  appendTo     the buffer where to append the valid characters.
     * @param  separator    the separator to append before the valid characters, or 0 if none.
     * @param  text         the text from which to get the valid character to append in the given buffer.
     * @param  accepted     additional characters to accept (e.g. {@code "-."}), or an empty string if none.
     * @param  toLowerCase  {@code true} for converting the characters to lower case.
     * @return {@code true} if at least one character has been added to the buffer.
     */
    public static boolean appendUnicodeIdentifier(final StringBuilder appendTo, final char separator,
            final String text, final String accepted, final boolean toLowerCase)
    {
        boolean added = false;
        boolean toUpperCase = false;
        if (text != null) {
            for (int i=0; i<text.length();) {
                final int c = text.codePointAt(i);
                final boolean isFirst = appendTo.length() == 0;
                if ((isFirst ? Character.isUnicodeIdentifierStart(c)
                             : Character.isUnicodeIdentifierPart(c)) || accepted.indexOf(c) >= 0)
                {
                    if (!isFirst && !added && separator != 0) {
                        appendTo.append(separator);
                    }
                    appendTo.appendCodePoint(toLowerCase ? Character.toLowerCase(c) :
                                             toUpperCase ? Character.toUpperCase(c) : c);
                    added = true;
                    toUpperCase = false;
                } else {
                    toUpperCase = true;
                }
                i += Character.charCount(c);
            }
        }
        return added;
    }

    /**
     * Appends {@code "[index]"} to the given name. This is used for formatting error messages.
     *
     * @param  name   the variable name to which to append "[index]".
     * @param  index  value to write between brackets.
     * @return {@code "name[index]"}.
     */
    public static String toIndexed(final String name, final int index) {
        return name + '[' + index + ']';
    }

    /**
     * Formats {@code "name[index]"}.
     *
     * @param  name   the variable name to which to append "[index]".
     * @param  index  value to write between brackets.
     * @return {@code "name[index]"}.
     */
    public static String bracket(final String name, final Object index) {
        if (index instanceof CharSequence) {
            return name + "[“" + index + "”]";
        } else {
            return name + '[' + index + ']';
        }
    }

    /**
     * Formats {@code "classname[index]"}.
     *
     * @param  type   the type to which to append "[index]".
     * @param  index  value to write between brackets.
     * @return {@code "classname[index]"}.
     */
    public static String bracket(final Class<?> type, final Object index) {
        return bracket(Classes.getShortName(type), index);
    }

    /**
     * Formats {@code "classname[lower … upper]"}.
     *
     * @param  type   the type to which to append "[lower … upper]".
     * @param  lower  first value to write between brackets.
     * @param  upper  second value to write between brackets.
     * @return {@code "classname[lower … upper]"}.
     */
    public static String range(final Class<?> type, final Object lower, final Object upper) {
        return Classes.getShortName(type) + '[' + lower + " … " + upper + ']';
    }

    /**
     * Returns a string with the same content than the given string, but in upper case and containing only the
     * filtered characters. If the given string already matches the criterion, then it is returned unchanged
     * without creation of any temporary object.
     *
     * <p>This method is useful before call to an {@code Enum.valueOf(String)} method, for making the search
     * a little bit more tolerant.</p>
     *
     * <p>This method is not in public API because conversion to upper-cases should be locale-dependent.</p>
     *
     * @param  text     the text to filter.
     * @param  filter   the filter to apply.
     * @return the filtered text.
     */
    public static String toUpperCase(final String text, final Characters.Filter filter) {
        final int length = text.length();
        int c, i = 0;
        while (true) {
            if (i >= length) {
                return text;
            }
            c = text.codePointAt(i);
            if (!filter.contains(c) || Character.toUpperCase(c) != c) {
                break;
            }
            i += Character.charCount(c);
        }
        /*
         * At this point we found that characters starting from index i does not match the criterion.
         * Copy what we have checked so far in the buffer, then add next characters one-by-one.
         */
        final StringBuilder buffer = new StringBuilder(length).append(text, 0, i);
        while (i < length) {
            c = text.codePointAt(i);
            if (filter.contains(c)) {
                buffer.appendCodePoint(Character.toUpperCase(c));
            }
            i += Character.charCount(c);
        }
        return buffer.toString();
    }

    /**
     * Inserts a continuation character after each line separator except the last one.
     * The intent is to show that a block of lines are part of the same element.
     * The characters are the same than {@link org.apache.sis.util.logging.MonolineFormatter}.
     *
     * @param buffer         the buffer where to insert a continuation character in the left margin.
     * @param lineSeparator  the line separator.
     */
    public static void insertLineInLeftMargin(final StringBuilder buffer, final String lineSeparator) {
        char c = CONTINUATION_END;
        int i = CharSequences.skipTrailingWhitespaces(buffer, 0, buffer.length());
        while ((i = buffer.lastIndexOf(lineSeparator, i - 1)) >= 0) {
            buffer.insert(i + lineSeparator.length(), c);
            c = CONTINUATION_MARK;
        }
    }

    /**
     * Returns a string representation of an instance of the given class having the given properties.
     * This is a convenience method for implementation of {@link Object#toString()} methods that are
     * used mostly for debugging purpose.
     *
     * <p>The content is specified by (<var>key</var>=<var>value</var>) pairs. If a value is {@code null},
     * the whole entry is omitted. If a key is {@code null}, the value is written without the {@code "key="}
     * part. The later happens typically when the first value is the object name.</p>
     *
     * @param  classe      the class to format.
     * @param  properties  the (<var>key</var>=<var>value</var>) pairs.
     * @return a string representation of an instance of the given class having the given properties.
     */
    public static String toString(final Class<?> classe, final Object... properties) {
        final StringBuffer buffer = new StringBuffer(32).append(Classes.getShortName(classe)).append('[');
        boolean isNext = false;
        for (int i=0; i<properties.length; i++) {
            final Object value = properties[++i];
            if (value != null) {
                if (isNext) {
                    buffer.append(", ");
                }
                final Object name = properties[i-1];
                if (name != null) {
                    buffer.append(name).append('=');
                }
                final boolean isText = (value instanceof CharSequence);
                if (isText) buffer.append('“');
                buffer.append(value);
                if (isText) buffer.append('”');
                isNext = true;
            }
        }
        return buffer.append(']').toString();
    }

    /**
     * Formats the given character sequence to the given formatter. This method takes in account
     * the {@link FormattableFlags#UPPERCASE} and {@link FormattableFlags#LEFT_JUSTIFY} flags.
     *
     * @param formatter  the formatter in which to format the value.
     * @param flags      the formatting flags.
     * @param width      minimal number of characters to write, padding with {@code ' '} if necessary.
     * @param precision  number of characters to keep before truncation, or -1 if no limit.
     * @param value      the text to format.
     */
    public static void formatTo(final Formatter formatter, final int flags, int width, int precision, String value) {
        final String format;
        final Object[] args;
        boolean isUpperCase = (flags & FormattableFlags.UPPERCASE) != 0;
        if (isUpperCase && width > 0) {
            // May change the string length in some locales.
            value = value.toUpperCase(formatter.locale());
            isUpperCase = false;                            // Because conversion has already been done.
        }
        int length = value.length();
        if (precision >= 0) {
            for (int i=0,n=0; i<length; i += n) {
                if (--precision < 0) {
                    /*
                     * Found the amount of characters to keep. The 'n' variable can be
                     * zero only if precision == 0, in which case the string is empty.
                     */
                    if (n == 0) {
                        value = "";
                    } else {
                        length = (i -= n) + 1;
                        final StringBuilder buffer = new StringBuilder(length);
                        value = buffer.append(value, 0, i).append('…').toString();
                    }
                    break;
                }
                n = Character.charCount(value.codePointAt(i));
            }
        }
        // Double check since length() is faster than codePointCount(…).
        if (width > length && (width -= value.codePointCount(0, length)) > 0) {
            format = "%s%s";
            args = new Object[] {value, value};
            args[(flags & FormattableFlags.LEFT_JUSTIFY) != 0 ? 1 : 0] = CharSequences.spaces(width);
        } else {
            format = isUpperCase ? "%S" : "%s";
            args = new Object[] {value};
        }
        formatter.format(format, args);
    }
}
