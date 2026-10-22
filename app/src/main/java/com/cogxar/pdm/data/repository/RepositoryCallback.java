package com.cogxar.pdm.data.repository;

/**
 * Pure Java asynchronous callback interface.
 * Intentionally free of any Android framework dependencies.
 */
public interface RepositoryCallback<T> {
    void onSuccess(T result);
    void onError(Throwable throwable);
}
