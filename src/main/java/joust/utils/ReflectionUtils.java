package joust.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class ReflectionUtils {
    /**
     * Get an array of all fields, irrespective of modifier, of a given Class.
     * @param pClass Class to get a field array for.
     * @return An array of Field objects representing every field in the given class.
     */
    public static Field[] getAllFields(Class<?> pClass) {
        ArrayList<Field> fds = new ArrayList<>();
        // Collect *all* the fields;
        Class<?> clazz = pClass;
        while (clazz != null) {
            Field[] fs = clazz.getDeclaredFields();
            for (int i = 0; i < fs.length; i++) {
                fds.add(fs[i]);
            }

            clazz = clazz.getSuperclass();
        }

        return fds.toArray(new Field[fds.size()]);
    }

    /**
     * Find a field with the given name on the given class, regardless of if it is declared on the class or
     * on of its superclasses.
     */
    public static Field findField(Class<?> pClass, String fieldName) throws NoSuchFieldException {
        Field[] fields = getAllFields(pClass);
        for (int i = 0; i < fields.length; i++) {
            if (fields[i].getName().equals(fieldName)) {
                return fields[i];
            }
        }

        throw new NoSuchFieldException("Unable to find field " + fieldName + " on class " + pClass.getCanonicalName());
    }
}
