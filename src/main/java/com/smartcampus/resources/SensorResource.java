package com.smartcampus.resources;

import com.smartcampus.exceptions.LinkedResourceNotFoundException;
import com.smartcampus.exceptions.ResourceNotFoundException;
import com.smartcampus.models.Sensor;
import com.smartcampus.util.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.stream.Collectors;
/**
 *
 * @author ehsaanzxk
 */
@Path("/api/v1/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {
    
    private final DataStore store = DataStore.getInstance();

    // GET /api/v1/sensors - list all sensors, with optional ?type= filter
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        List<Sensor> sensors = new ArrayList<>(store.getSensors().values());

        // Apply optional type filter
        if (type != null && !type.isEmpty()) {
            sensors = sensors.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return Response.ok(sensors).build();
    }

    // GET /api/v1/sensors/{sensorId} - get a specific sensor
    @GET
    @Path("/{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor with ID '" + sensorId + "' was not found.");
        }
        return Response.ok(sensor).build();
    }

    // POST /api/v1/sensors - register a new sensor
    @POST
    public Response createSensor(Sensor sensor) {
        // Validate that the referenced room actually exists
        if (sensor.getRoomId() == null || store.getRoom(sensor.getRoomId()) == null) {
            throw new LinkedResourceNotFoundException(
                "The roomId '" + sensor.getRoomId() + "' does not refer to an existing room. " +
                "Please create the room first before registering a sensor to it."
            );
        }

        if (sensor.getId() == null || sensor.getId().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "Sensor 'id' field is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        if (store.getSensor(sensor.getId()) != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", 409);
            error.put("error", "Conflict");
            error.put("message", "A sensor with ID '" + sensor.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        // Default status if not provided
        if (sensor.getStatus() == null) {
            sensor.setStatus("ACTIVE");
        }

        // Register sensor and link it to its room
        store.addSensor(sensor);
        store.getRoom(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor registered successfully.");
        response.put("sensor", sensor);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // DELETE /api/v1/sensors/{sensorId}
    @DELETE
    @Path("/{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor with ID '" + sensorId + "' was not found.");
        }

        // Unlink from room
        if (sensor.getRoomId() != null && store.getRoom(sensor.getRoomId()) != null) {
            store.getRoom(sensor.getRoomId()).getSensorIds().remove(sensorId);
        }

        store.deleteSensor(sensorId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Sensor '" + sensorId + "' has been removed.");
        return Response.ok(response).build();
    }

    /**
     * Sub-resource locator for sensor readings.
     * Delegates all /api/v1/sensors/{sensorId}/readings/* requests to SensorReadingResource.
     * No HTTP method annotation = this is a locator, not an endpoint itself.
     */
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingResource(@PathParam("sensorId") String sensorId) {
        // Validate the sensor exists before delegating
        Sensor sensor = store.getSensor(sensorId);
        if (sensor == null) {
            throw new ResourceNotFoundException("Sensor with ID '" + sensorId + "' was not found.");
        }
        return new SensorReadingResource(sensorId);
    }
}
