/**
 * Slightly altererd edition of the JLibs ANSI library.
 * Needed to break the dependency on jlibs-core because its annotation processors were causing
 * compiler warnings that throw off attempts to build some projects with the optimiser.
 * Consider breaking your libraries up, guys!
 */

/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package joust.utils.logging.ansi;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author Santhosh Kumar T
 */
public class PreciseFormatter extends Formatter {
    private final CharArrayWriter writer = new CharArrayWriter();

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public String format(LogRecord record) {

        try {
            writer.write(record.getLevel().toString());
            writer.write('\t');
            writer.write(':');
            writer.write(formatMessage(record));
            writer.write('\n');
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        if (record.getThrown() != null) {
            PrintWriter printer = new PrintWriter(writer);
            record.getThrown().printStackTrace(printer);
            printer.close();
        }

        String result = writer.toString();
        writer.reset();
        return result;
    }
}
