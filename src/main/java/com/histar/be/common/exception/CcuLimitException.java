package com.histar.be.common.exception;

public class CcuLimitException extends RuntimeException {

    private final int maxCcu;
    private final int currentCcu;

    public CcuLimitException(String message, int maxCcu, int currentCcu) {
        super(message);
        this.maxCcu = maxCcu;
        this.currentCcu = currentCcu;
    }

    public int getMaxCcu() {
        return maxCcu;
    }

    public int getCurrentCcu() {
        return currentCcu;
    }
}
