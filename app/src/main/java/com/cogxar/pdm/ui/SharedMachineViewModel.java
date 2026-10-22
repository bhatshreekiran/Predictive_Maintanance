package com.cogxar.pdm.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.cogxar.pdm.data.model.Machine;

/**
 * Activity-scoped ViewModel to synchronize the selected machine between Dashboard and History.
 */
public class SharedMachineViewModel extends ViewModel {

    private final MutableLiveData<Machine> selectedMachine = new MutableLiveData<>();

    @NonNull
    public LiveData<Machine> getSelectedMachine() {
        return selectedMachine;
    }

    public void selectMachine(@Nullable Machine machine) {
        if (machine != null) {
            Machine current = selectedMachine.getValue();
            if (current == null || !machine.getId().equals(current.getId())) {
                selectedMachine.setValue(machine);
            }
        }
    }
}
