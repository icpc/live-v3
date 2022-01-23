package org.icpclive.events;

public enum OptimismLevel {
    NORMAL,
    OPTIMISTIC,
    PESSIMISTIC;

    public String toString() {
        switch (this) {
            case NORMAL:
                return "Normal";
            case OPTIMISTIC:
                return "Optimistic";
            case PESSIMISTIC:
                return "Pessimistic";

            default:
                throw new IllegalArgumentException();
        }
    }
}
