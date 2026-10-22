package com.cogxar.pdm.debug;

import android.content.Context;
import android.view.View;
import com.cogxar.pdm.data.repository.PdmRepository;

/**
 * Release no-op stub ensuring no debug UI or reflection exists in production builds.
 */
public class DebugVariantPicker {

    public static void setup(Context context, View triggerView, PdmRepository repository, Runnable onVariantChanged) {
        if (triggerView != null) {
            triggerView.setVisibility(View.GONE);
        }
    }
}
