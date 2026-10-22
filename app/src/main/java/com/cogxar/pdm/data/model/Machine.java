package com.cogxar.pdm.data.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.gson.annotations.SerializedName;
import java.util.Objects;

/**
 * Represents an industrial machine being monitored.
 */
public class Machine {
    @SerializedName("id")
    @NonNull
    private final String id;

    @SerializedName("name")
    @NonNull
    private final String name;

    @SerializedName("location")
    @Nullable
    private final String location; // Optional future field

    public Machine(@NonNull String id, @NonNull String name) {
        this(id, name, null);
    }

    public Machine(@NonNull String id, @NonNull String name, @Nullable String location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }

    @NonNull
    public String getId() {
        return id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Machine machine = (Machine) o;
        return Objects.equals(id, machine.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}
