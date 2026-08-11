package com.aliyun.openservices.pairec.recallengine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * InsertMode defines the write mode for recall engine.
 */
public enum InsertMode {
    /**
     * Insert mode (default): writes the record as a whole row.
     *
     * <p>A record whose primary key already exists is <em>replaced</em>, not
     * rejected. Because the replacement covers the whole row, any field absent
     * from the request is dropped from the stored record, so a request that
     * carries only part of the columns clears the ones it omits. Supported by
     * every table type.
     */
    INSERT("insert"),

    /**
     * Upsert mode: merges the record into the existing row.
     *
     * <p>Fields carried by the request overwrite their stored counterparts,
     * while fields absent from the request keep their current values. This is
     * the mode to use for partial updates. Only KV and Vector tables support
     * upsert; other table types reject the request.
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

    /**
     * Parses the given string into an InsertMode.
     *
     * <p>A null input falls back to the default {@link #INSERT} mode, which keeps
     * requests without an explicit insert_mode backward compatible. Any other
     * unrecognized value is rejected instead of silently degrading to INSERT,
     * so that a misconfigured insert_mode (for example a typo in a Flink DDL
     * option) fails fast rather than changing the write semantics unnoticed.
     *
     * @param value the mode name, case-insensitive and surrounding whitespace tolerant
     * @return the matching InsertMode, or INSERT when value is null
     * @throws IllegalArgumentException if value is non-null but not a supported mode
     */
    @JsonCreator
    public static InsertMode fromValue(String value) {
        if (value == null) {
            return INSERT;
        }
        String normalized = value.trim();
        for (InsertMode mode : InsertMode.values()) {
            if (mode.value.equalsIgnoreCase(normalized)) {
                return mode;
            }
        }
        throw new IllegalArgumentException(
                "Unsupported insert_mode: '" + value + "'. Supported values: " + supportedValues());
    }

    /**
     * Returns the comma separated list of accepted insert_mode values, used in error messages.
     */
    private static String supportedValues() {
        StringBuilder sb = new StringBuilder();
        for (InsertMode mode : InsertMode.values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(mode.value);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return value;
    }
}
