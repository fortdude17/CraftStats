package com.craftstats.common.gui.panel;

public record FieldDef(String fieldName, String label, FieldType type, String[] options) {

    public enum FieldType { NUMBER, TOGGLE, PILL, TEXT }

    public static FieldDef num(String field, String label) {
        return new FieldDef(field, label, FieldType.NUMBER, null);
    }

    public static FieldDef toggle(String field, String label) {
        return new FieldDef(field, label, FieldType.TOGGLE, null);
    }

    public static FieldDef pill(String field, String label, String[] options) {
        return new FieldDef(field, label, FieldType.PILL, options);
    }

    public static FieldDef text(String field, String label) {
        return new FieldDef(field, label, FieldType.TEXT, null);
    }
}
