package com.cogxar.pdm.data.repository;

import com.cogxar.pdm.BuildConfig;
import com.cogxar.pdm.data.remote.RetrofitClient;

/**
 * Factory providing the active repository implementation based on BuildConfig.USE_MOCK.
 */
public class PdmRepositoryFactory {

    private static volatile PdmRepository instance;

    public static PdmRepository getInstance() {
        if (instance == null) {
            synchronized (PdmRepositoryFactory.class) {
                if (instance == null) {
                    if (BuildConfig.USE_MOCK) {
                        instance = new MockPdmRepository();
                    } else {
                        instance = new RemotePdmRepository(RetrofitClient.getApiService());
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Helper for debug tools and testing to inject or retrieve the mock repository.
     */
    public static void setInstance(PdmRepository repository) {
        synchronized (PdmRepositoryFactory.class) {
            instance = repository;
        }
    }
}
