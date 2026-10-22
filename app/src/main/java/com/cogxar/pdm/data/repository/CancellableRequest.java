package com.cogxar.pdm.data.repository;

/**
 * Handle allowing cancellation of an in-flight repository operation.
 */
public interface CancellableRequest {
    void cancel();
    boolean isCanceled();

    CancellableRequest NO_OP = new CancellableRequest() {
        @Override
        public void cancel() {}

        @Override
        public boolean isCanceled() {
            return false;
        }
    };
}
