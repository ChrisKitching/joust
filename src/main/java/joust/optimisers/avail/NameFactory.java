package joust.optimisers.avail;

import com.sun.tools.javac.util.Name;

import static joust.Optimiser.names;

/**
 * Class for creating unique character strings for use as temporary variable names.
 */
public class NameFactory {
    // The next name to generate.
    private static char[] sourceCharacters = new char[]{'a'};

    /**
     * Return a new unique alphanumeric name.
     *
     * @return An alphanumeric string that has never before been returned from this function.
     */
    public static String getStringName() {
        String name = "$JOU$T$" + new String(sourceCharacters);

        // Update the character array to the next string.
        int modificationIndex = sourceCharacters.length - 1;
        sourceCharacters[modificationIndex]++;

        // Gone past 'z' on the last character - run back up the array cascading the characters.
        while (sourceCharacters[modificationIndex] > 'z') {
            sourceCharacters[modificationIndex] = 'a';
            modificationIndex--;
            sourceCharacters[modificationIndex]++;

            // We reached the front of the array before managing to apply the increment - we've
            // exhausted all combinations of this length. Enlarge the array.
            if (modificationIndex == -1) {
                // Exhausted!
                char[] newChars = new char[sourceCharacters.length+1];
                System.arraycopy(sourceCharacters, 0, newChars, 0, sourceCharacters.length);
                newChars[newChars.length-1] = 'a';
                sourceCharacters = newChars;
                break;
            }
        }

        return name;
    }

    public static Name getName() {
        return names.fromString(getStringName());
    }
}
