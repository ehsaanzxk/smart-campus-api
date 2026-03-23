package com.smartcampus.resources;

import com.smartcampus.exceptions.SensorUnavailableException;
import com.smartcampus.models.Sensor;
import com.smartcampus.models.SensorReading;
import com.smartcampus.util.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 *
 * @author ehsaanzxk
 */

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class SensorReadingResource {

    private final String sensorId;
    private final DataStore store = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET /api/v1/sensors/{sensorId}/readings - retrieve all historical readings
    @GET
    public Response getReadings() {
        List<SensorReading> readings = store.getReadings(sensorId);

        Map<String, Object> response = new HashMap<>();
        response.put("sensorId", sensorId);
        response.put("count", readings.size());
        response.put("readings", readings);

        return Response.ok(response).build();
    }

    // POST /api/v1/sensors/{sensorId}/readings - log a new reading
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = store.getSensor(sensorId);

        // Block if sensor is under maintenance
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId);
        }

        // Auto-generate id and timestamp if not provided
        if (reading.getId() == null || reading.getId().isEmpty()) {
            SensorReading generated = new SensorReading(reading.getValue());
            reading.setId(generated.getId());
            reading.setTimestamp(generated.getTimestamp());
        }

        // Store the reading
        store.addReading(sensorId, reading);

        // Side effect: update sensor's currentValue to the latest reading
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Reading recorded successfully.");
        response.put("sensorId", sensorId);
        response.put("updatedCurrentValue", sensor.getCurrentValue());
        response.put("reading", reading);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // GET /api/v1/sensors/{sensorId}/readings/{readingId} - get a specific reading
    @GET
    @Path("/{readingId}")
    public Response getReadingById(@PathParam("readingId") String readingId) {
        List<SensorReading> readings = store.getReadings(sensorId);

        return readings.stream()
                .filter(r -> r.getId().equals(readingId))
                .findFirst()
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of(
                            "status", 404,
                            "error", "Not Found",
                            "message", "Reading '" + readingId + "' not found for sensor '" + sensorId + "'."
                        )).build());
    }
}
