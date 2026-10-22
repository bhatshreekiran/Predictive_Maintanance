package com.cogxar.pdm.debug;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;
import com.cogxar.pdm.R;
import com.cogxar.pdm.data.mock.MockVariant;
import com.cogxar.pdm.data.repository.MockPdmRepository;
import com.cogxar.pdm.data.repository.PdmRepository;

/**
 * Debug-only controller for switching mock scenarios on the fly.
 * Excluded from release APKs.
 */
public class DebugVariantPicker {

    public static void setup(Context context, View triggerView, PdmRepository repository, Runnable onVariantChanged) {
        if (triggerView == null || !(repository instanceof MockPdmRepository)) {
            return;
        }

        MockPdmRepository mockRepo = (MockPdmRepository) repository;
        triggerView.setVisibility(View.VISIBLE);
        triggerView.setOnClickListener(v -> showDialog(context, mockRepo, onVariantChanged));
    }

    private static void showDialog(Context context, MockPdmRepository mockRepo, Runnable onVariantChanged) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_debug_variant_picker, null);
        RadioGroup radioGroup = dialogView.findViewById(R.id.rg_mock_variants);

        MockVariant current = mockRepo.getVariant();
        if (current == MockVariant.NORMAL) {
            radioGroup.check(R.id.rb_variant_normal);
        } else if (current == MockVariant.ANOMALY_FLAGGED) {
            radioGroup.check(R.id.rb_variant_anomaly);
        } else if (current == MockVariant.DEVICE_OFFLINE) {
            radioGroup.check(R.id.rb_variant_offline);
        } else if (current == MockVariant.STALE_DATA) {
            radioGroup.check(R.id.rb_variant_stale);
        } else if (current == MockVariant.EMPTY) {
            radioGroup.check(R.id.rb_variant_empty);
        } else if (current == MockVariant.SERVER_ERROR) {
            radioGroup.check(R.id.rb_variant_error);
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton("Apply", (d, which) -> {
                    int checkedId = radioGroup.getCheckedRadioButtonId();
                    MockVariant selectedVariant = MockVariant.NORMAL;
                    if (checkedId == R.id.rb_variant_anomaly) {
                        selectedVariant = MockVariant.ANOMALY_FLAGGED;
                    } else if (checkedId == R.id.rb_variant_offline) {
                        selectedVariant = MockVariant.DEVICE_OFFLINE;
                    } else if (checkedId == R.id.rb_variant_stale) {
                        selectedVariant = MockVariant.STALE_DATA;
                    } else if (checkedId == R.id.rb_variant_empty) {
                        selectedVariant = MockVariant.EMPTY;
                    } else if (checkedId == R.id.rb_variant_error) {
                        selectedVariant = MockVariant.SERVER_ERROR;
                    }
                    mockRepo.setVariant(selectedVariant);
                    if (onVariantChanged != null) {
                        onVariantChanged.run();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }
}
