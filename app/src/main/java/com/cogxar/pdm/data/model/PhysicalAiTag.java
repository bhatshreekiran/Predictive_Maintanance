package com.cogxar.pdm.data.model;

import androidx.annotation.NonNull;
import com.google.gson.annotations.SerializedName;

/**
 * Analog Physical AI 4-bit encoder/tag (integer 0-15).
 * Prepared for future physical AI classification display.
 */
public class PhysicalAiTag {
    @SerializedName("value")
    private final int value; // 0 to 15

    public PhysicalAiTag(int value) {
        if (value < 0 || value > 15) {
            throw new IllegalArgumentException("Tag value must be between 0 and 15");
        }
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Returns 4-bit binary representation (e.g. "0101").
     */
    @NonNull
    public String getBitString() {
        return String.format("%4s", Integer.toBinaryString(value)).replace(' ', '0');
    }

    /**
     * Returns hexadecimal representation (e.g. "0x5").
     */
    @NonNull
    public String getHexString() {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }
}
