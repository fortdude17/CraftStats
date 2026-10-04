package com.craftstats.common.stats;

/**
 * One editable stat: the Java field on the stats object plus everything the editor,
 * server-side validation and randomizing need to know about it.
 */
public record StatDef(String field, String label, Kind kind, double min, double max, double fallback,
                      String[] options, String help, int flags) {

    public enum Kind { NUMBER, TOGGLE, CHOICE, TEXT }

    /** Blocks: Minecraft has no per-position hook, so this only works for the whole block type. */
    public static final int TYPE_ONLY = 1;
    /** Included when randomizing. */
    public static final int RANDOM = 2;
    /** Text fields holding an id, used for validation and suggestions. */
    public static final int ID_EFFECT = 4, ID_ENTITY = 8, ID_ITEM = 16;

    public static StatDef num(String field, String label, double min, double max) {
        return new StatDef(field, label, Kind.NUMBER, min, max, Double.NaN, null, "", 0);
    }

    public static StatDef toggle(String field, String label) {
        return new StatDef(field, label, Kind.TOGGLE, 0, 1, Double.NaN, null, "", 0);
    }

    public static StatDef choice(String field, String label, String... options) {
        return new StatDef(field, label, Kind.CHOICE, 0, 0, Double.NaN, options, "", 0);
    }

    public static StatDef text(String field, String label, int idFlag) {
        return new StatDef(field, label, Kind.TEXT, 0, 0, Double.NaN, null, "", idFlag);
    }

    /** Tooltip text. */
    public StatDef help(String text) {
        return new StatDef(field, label, kind, min, max, fallback, options, text, flags);
    }

    /** The value shown as "default" when the target has no specific vanilla value. */
    public StatDef def(double value) {
        return new StatDef(field, label, kind, min, max, value, options, help, flags);
    }

    public StatDef typeOnly() {
        return new StatDef(field, label, kind, min, max, fallback, options, help, flags | TYPE_ONLY);
    }

    public StatDef random() {
        return new StatDef(field, label, kind, min, max, fallback, options, help, flags | RANDOM);
    }

    public boolean has(int flag) { return (flags & flag) != 0; }

    public boolean perPosition() { return !has(TYPE_ONLY); }
}
