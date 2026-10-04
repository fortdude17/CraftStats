package com.craftstats.common.stats;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Reads and writes stats fields by name, and keeps values inside the ranges in {@link StatCatalog}. */
public final class StatAccess {

    private static final Map<Class<?>, Map<String, Field>> FIELDS = new ConcurrentHashMap<>();
    private static final int MAX_TEXT = 256;

    private StatAccess() {}

    public static Field field(Class<?> type, String name) {
        return FIELDS.computeIfAbsent(type, t -> new ConcurrentHashMap<>()).computeIfAbsent(name, n -> {
            try {
                return type.getField(n);
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException("No field '" + n + "' on " + type.getSimpleName(), e);
            }
        });
    }

    public static Object get(Object stats, String name) {
        if (stats == null) return null;
        try {
            return field(stats.getClass(), name).get(stats);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static void set(Object stats, String name, Object value) {
        try {
            field(stats.getClass(), name).set(stats, value);
        } catch (IllegalAccessException | IllegalArgumentException ignored) {
        }
    }

    public static Class<?> typeOf(Object stats, String name) {
        return field(stats.getClass(), name).getType();
    }

    public static boolean isInteger(Class<?> t) {
        return t == int.class || t == Integer.class || t == long.class || t == Long.class;
    }

    /** Converts a double to the field's numeric type (boxed if the field is boxed). */
    public static Object coerce(Class<?> t, double v) {
        if (t == double.class || t == Double.class) return v;
        if (t == float.class || t == Float.class) return (float) v;
        if (t == int.class || t == Integer.class) return (int) Math.round(v);
        if (t == long.class || t == Long.class) return Math.round(v);
        throw new IllegalArgumentException("not a number type: " + t);
    }

    /**
     * Clamps numbers into their allowed range, replaces unknown choices with the first option
     * and trims text. Run on the server for everything a client sends.
     */
    public static void sanitize(TargetType type, Object stats) {
        for (StatDef def : StatCatalog.all(type)) {
            Object v = get(stats, def.field());
            switch (def.kind()) {
                case NUMBER -> {
                    if (!(v instanceof Number n)) continue;
                    double d = n.doubleValue();
                    if (Double.isNaN(d) || Double.isInfinite(d)) d = def.min();
                    double clamped = Math.max(def.min(), Math.min(def.max(), d));
                    if (clamped != d) set(stats, def.field(), coerce(typeOf(stats, def.field()), clamped));
                }
                case CHOICE -> {
                    if (!(v instanceof String s) || !Arrays.asList(def.options()).contains(s))
                        set(stats, def.field(), def.options()[0]);
                }
                case TEXT -> {
                    String s = v instanceof String str ? str.trim() : "";
                    if (s.length() > MAX_TEXT) s = s.substring(0, MAX_TEXT);
                    if (!s.equals(v)) set(stats, def.field(), s);
                }
                case TOGGLE -> {}
            }
        }
    }

    /** Number of stats that differ from a fresh (unmodified) object. */
    public static int countChanged(TargetType type, Object stats, Iterable<StatDef> defs) {
        if (stats == null) return 0;
        Object fresh = StatSchema.empty(type);
        int n = 0;
        for (StatDef def : defs) {
            Object a = get(stats, def.field()), b = get(fresh, def.field());
            if (a == null ? b != null : !a.equals(b)) n++;
        }
        return n;
    }
}
