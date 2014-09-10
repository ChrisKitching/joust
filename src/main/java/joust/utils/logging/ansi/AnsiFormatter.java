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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * This is an implementation of {@link java.util.logging.Formatter}, to use ansi colors in logging.
 * <p/>
 * Example Usage: <pre class="prettyprint"> Logger logger = LogManager.getLogManager().getLogger("");
 * logger.setLevel(Level.FINEST);
 * <p/>
 * Handler handler = logger.getHandlers()[0]; handler.setLevel(Level.FINEST);
 * handler.setFormatter(new {@link AnsiFormatter}());
 * <p/>
 * for(Level level: map.keySet()) logger.log(level, "this is "+level+" message"); * </pre> </pre>
 * <p/>
 * This class has public final constants to access Ansi instance used for each level.<br> These
 * constants are made public, so that you can use them any where. for example you can do: <pre
 * class="prettyprint"> import static jlibs.core.util.logging.AnsiFormatter.*;
 * <p/>
 * {@link #SEVERE}.out("User authentication failed"); </pre>
 * <p/>
 * The colors used by AnsiFormatter for any level can be changed to match you taste. To do this you
 * need to create a properties file.<br> for example: <pre class="prettyprint"> # myansi.properties
 * SEVERE=DIM;RED;GREEN WARNING=BRIGHT;RED;YELLOW </pre> Now use following system property: <pre
 * class="prettyprint"> -Dansiformatter.default=/path/to/myansi.properties </pre> Each entry in this
 * property file is to be given as below: <pre class="prettyprint"> LEVEL=Attribute[;Foreground[;Background]]
 * </pre> key is the level name;<br> value is semicolon(;) separated values, where where tokens are
 * attribute, foreground and background respectively.<br> if any non-trailing token in value is
 * null, you still need to specify empty value. for example: <pre class="prettyprint">
 * SEVERE=DIM;;GREEN # foreground is not specified </pre> In your properties file, you don't need to
 * specify entries for each level. you can specify entries only for those levels that you want to
 * change.
 *
 * @author Santhosh Kumar T
 * @see Ansi
 */
public class AnsiFormatter extends Formatter {
    private static final Map<Level, Ansi> map = new LinkedHashMap<Level, Ansi>();

    private static final Map<Level, String> PROPERTIES = new HashMap<Level, String>() {
        {
            put(Level.SEVERE, "DIM;RED");
            put(Level.WARNING, "BRIGHT;RED");
            put(Level.INFO, "DIM;CYAN");
            put(Level.CONFIG, "DIM;MAGENTA");
            put(Level.FINE, "DIM;GREEN");
            put(Level.FINER, "DIM;YELLOW");
            put(Level.FINEST, "BRIGHT;YELLOW");
        }
    };

    static {
        for (Map.Entry<Level, String> levelStringEntry : PROPERTIES.entrySet()) {
            map.put(levelStringEntry.getKey(), new Ansi(levelStringEntry.getValue()));
        }
    }

    public static final Ansi SEVERE = map.get(Level.SEVERE);
    public static final Ansi WARNING = map.get(Level.WARNING);
    public static final Ansi INFO = map.get(Level.INFO);
    public static final Ansi CONFIG = map.get(Level.CONFIG);
    public static final Ansi FINE = map.get(Level.FINE);
    public static final Ansi FINER = map.get(Level.FINER);
    public static final Ansi FINEST = map.get(Level.FINEST);

    private final Formatter delegate;

    public AnsiFormatter(Formatter delegate) {
        this.delegate = delegate;
    }

    public AnsiFormatter() {
        this(new PreciseFormatter());
    }

    @Override
    public String format(LogRecord record) {
        return map.get(record.getLevel()).colorize(delegate.format(record));
    }
}

