package com.northbridge.casevault.model;

public enum Status {
    OPEN, IN_PROGRESS, AWAITING_APPROVAL, CLOSED;

    // The state machine: which status may follow which
    public boolean canMoveTo(Status next) {
        switch (this) {
            case OPEN:
                return next == IN_PROGRESS;
            case IN_PROGRESS:
                return next == AWAITING_APPROVAL;
            case AWAITING_APPROVAL:
                return next == CLOSED || next == IN_PROGRESS;
            default:
                return false;
        }
    }
}