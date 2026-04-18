package io.github.jvmdoctor.domain;

public enum ApplicationLogSignalType {
    OUT_OF_MEMORY,
    GC_OVERHEAD,
    DB_CONNECTION_TIMEOUT,
    SQL_TIMEOUT,
    DOWNSTREAM_TIMEOUT,
    DEADLOCK,
    REJECTED_EXECUTION,
    LOGGING_OVERHEAD
}
