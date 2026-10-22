package com.cogxar.pdm.data.mock;

import com.cogxar.pdm.data.model.DashboardResponse;
import com.cogxar.pdm.data.model.Device;
import com.cogxar.pdm.data.model.DeviceStatus;
import com.cogxar.pdm.data.model.FlaggedEvent;
import com.cogxar.pdm.data.model.HistoricalReadingsResponse;
import com.cogxar.pdm.data.model.Machine;
import com.cogxar.pdm.data.model.PhysicalAiTag;
import com.cogxar.pdm.data.model.SensorReading;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Generates dynamic mock data matching the API contract for all 6 testing scenarios.
 */
public class MockDataSource {

    private final Random random = new Random();

    public List<Machine> getMachines() {
        return Arrays.asList(
                new Machine("pump-01", "Pump 01", "Sector 4 Water Treatment"),
                new Machine("compressor-02", "Compressor 02", "Assembly Line 2"),
                new Machine("motor-03", "Exhaust Motor 03", "Cooling Tower B")
        );
    }

    public DashboardResponse getDashboard(String machineId, MockVariant variant) throws IOException {
        if (variant == MockVariant.SERVER_ERROR) {
            throw new IOException("503 Service Unavailable: Telemetry bridge unreachable");
        }

        Machine targetMachine = null;
        for (Machine m : getMachines()) {
            if (m.getId().equalsIgnoreCase(machineId)) {
                targetMachine = m;
                break;
            }
        }
        if (targetMachine == null) {
            targetMachine = new Machine(machineId, "Machine " + machineId);
        }

        Instant now = Instant.now();

        if (variant == MockVariant.EMPTY) {
            Device device = new Device(DeviceStatus.RUNNING, now.toString());
            return new DashboardResponse(targetMachine, device, null, Collections.emptyList(), null);
        }

        if (variant == MockVariant.DEVICE_OFFLINE) {
            Instant fifteenMinsAgo = now.minusSeconds(900);
            Device device = new Device(DeviceStatus.OFFLINE, fifteenMinsAgo.toString());
            String readingTs = fifteenMinsAgo.toString();
            List<SensorReading> sensors = Arrays.asList(
                    new SensorReading("s1", "Temperature Sensor", "temperature", 45.0, "°C", readingTs),
                    new SensorReading("s2", "Vibration Sensor", "vibration", 0.0, "mm/s", readingTs)
            );
            return new DashboardResponse(targetMachine, device, readingTs, sensors, null);
        }

        if (variant == MockVariant.STALE_DATA) {
            // Last reading > STALE_THRESHOLD_SEC (30s), e.g. 65 seconds ago
            Instant readingTs = now.minusSeconds(65);
            Device device = new Device(DeviceStatus.RUNNING, now.minusSeconds(10).toString());
            List<SensorReading> sensors = Arrays.asList(
                    new SensorReading("s1", "Temperature Sensor", "temperature", 68.4, "°C", readingTs.toString()),
                    new SensorReading("s2", "Vibration Sensor", "vibration", 3.8, "mm/s", readingTs.toString())
            );
            return new DashboardResponse(targetMachine, device, readingTs.toString(), sensors, null);
        }

        if (variant == MockVariant.ANOMALY_FLAGGED) {
            Instant readingTs = now.minusSeconds(2);
            Device device = new Device(DeviceStatus.RUNNING, now.toString());
            List<SensorReading> sensors = Arrays.asList(
                    new SensorReading("s1", "Temperature Sensor", "temperature", 81.2, "°C", readingTs.toString()),
                    new SensorReading("s2", "Vibration Sensor", "vibration", 9.8, "mm/s", readingTs.toString())
            );
            FlaggedEvent flag = new FlaggedEvent(
                    "s2",
                    "Vibration Sensor",
                    "Abnormal vibration (9.8 mm/s exceeds device baseline)",
                    readingTs.toString()
            );
            return new DashboardResponse(targetMachine, device, readingTs.toString(), sensors, flag, new PhysicalAiTag(14));
        }

        // Default: NORMAL
        double tempJitter = (random.nextInt(10) - 5) * 0.1; // +/- 0.5
        double vibJitter = (random.nextInt(6) - 3) * 0.05;  // +/- 0.15
        double tempVal = Math.round((72.0 + tempJitter) * 10.0) / 10.0;
        double vibVal = Math.round((4.2 + vibJitter) * 10.0) / 10.0;

        Instant readingTs = now.minusSeconds(3);
        Device device = new Device(DeviceStatus.RUNNING, now.minusSeconds(1).toString());
        List<SensorReading> sensors = Arrays.asList(
                new SensorReading("s1", "Temperature Sensor", "temperature", tempVal, "°C", readingTs.toString()),
                new SensorReading("s2", "Vibration Sensor", "vibration", vibVal, "mm/s", readingTs.toString())
        );

        return new DashboardResponse(targetMachine, device, readingTs.toString(), sensors, null, new PhysicalAiTag(5));
    }

    public HistoricalReadingsResponse getReadings(String machineId, String timeRange, String cursor, MockVariant variant) throws IOException {
        if (variant == MockVariant.SERVER_ERROR) {
            throw new IOException("503 Service Unavailable: Could not load historical readings");
        }
        if (variant == MockVariant.EMPTY) {
            return new HistoricalReadingsResponse(Collections.emptyList(), null);
        }

        int count;
        long intervalSec;
        if ("1h".equalsIgnoreCase(timeRange)) {
            count = 60;
            intervalSec = 60L; // 1 min interval
        } else if ("7d".equalsIgnoreCase(timeRange)) {
            count = 84;
            intervalSec = 7200L; // 2 hour interval
        } else {
            // Default 24h
            count = 96;
            intervalSec = 900L; // 15 min interval
        }

        Instant now = Instant.now();
        List<HistoricalReadingsResponse.HistoricalPoint> points = new ArrayList<>(count);

        // Generate points from oldest to newest (chronological order)
        for (int i = count - 1; i >= 0; i--) {
            Instant ts = now.minusSeconds(i * intervalSec);
            // Introduce realistic anomaly markers at selected points
            boolean isAnomaly = (i == count / 5 || i == (3 * count) / 5);

            double baseTemp = 71.5 + Math.sin(i * 0.15) * 2.5;
            double baseVib = 4.2 + Math.cos(i * 0.12) * 0.4;

            double temp = isAnomaly ? 84.5 + (i % 3) * 1.5 : Math.round(baseTemp * 10.0) / 10.0;
            double vib = isAnomaly ? 8.8 + (i % 4) * 0.4 : Math.round(baseVib * 10.0) / 10.0;
            int tagVal = isAnomaly ? 14 : (i % 16);

            points.add(new HistoricalReadingsResponse.HistoricalPoint(
                    ts.toString(),
                    temp,
                    vib,
                    new PhysicalAiTag(tagVal),
                    isAnomaly
            ));
        }

        return new HistoricalReadingsResponse(points, null);
    }

    public HistoricalReadingsResponse getReadings(String machineId, String timeRange, String cursor) throws IOException {
        return getReadings(machineId, timeRange, cursor, MockVariant.NORMAL);
    }
}
