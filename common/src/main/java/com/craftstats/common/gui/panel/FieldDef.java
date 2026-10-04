package com.craftstats.common.gui.panel;

/**
 * One editable stat in the GUI. {@code fieldName} is the Java field on the stats object.
 * {@code perPosition} marks block fields that also work for a single block position.
 */
public record FieldDef(String fieldName, String label, FieldType type, String[] options, boolean perPosition) {

    public enum FieldType { NUMBER, TOGGLE, PILL, TEXT }

    public static FieldDef num(String field, String label)    { return new FieldDef(field, label, FieldType.NUMBER, null, true); }
    public static FieldDef toggle(String field, String label) { return new FieldDef(field, label, FieldType.TOGGLE, null, true); }
    public static FieldDef text(String field, String label)   { return new FieldDef(field, label, FieldType.TEXT, null, true); }
    public static FieldDef pill(String field, String label, String... options) {
        return new FieldDef(field, label, FieldType.PILL, options, true);
    }

    /** Marks a block field as type-wide only (Minecraft has no per-position hook for it). */
    public FieldDef typeOnly() {
        return new FieldDef(fieldName, label, type, options, false);
    }
}
