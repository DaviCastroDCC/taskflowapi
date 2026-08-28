package com.taskflow.api.common;

public final class Text {

    private Text() {
    }

    /** Trims the value and collapses blank input to {@code null} so optional columns stay empty rather than storing "". */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
