package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * InsertMode defines the write mode for recall engine.
 */
public enum InsertMode {
    /**
     * Insert mode: insert new records only, fail if record exists (default)
     */
    INSERT("insert"),

    /**
     * Upsert mode: insert new records or update existing ones
     */
    UPSERT("upsert");

    private final String value;

    InsertMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static InsertMode fromValue(String value) {
        if (value == null) {
            return INSERT;
        }
        for (InsertMode mode : InsertMode.values()) {
            if (mode.value.equalsIgnoreCase(value)) {
                return mode;
            }
        }
        // Default to INSERT for unknown values
        return INSERT;
    }

    @Override
    public String toString() {
        return value;
    }
}
