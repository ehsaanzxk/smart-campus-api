package com.smartcampus.resources;

import com.smartcampus.exceptions.ResourceNotFoundException;
import com.smartcampus.exceptions.RoomNotEmptyException;
import com.smartcampus.models.Room;
import com.smartcampus.util.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ehsaanzxk
 */
@Path("/api/v1/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)

public class RoomResource {


    private final DataStore store = DataStore.getInstance();

    // GET /api/v1/rooms - list all rooms
    @GET
    public Response getAllRooms() {
        List<Room> rooms = new ArrayList<>(store.getRooms().values());
        return Response.ok(rooms).build();
    }

    // GET /api/v1/rooms/{roomId} - get a specific room
    @GET
    @Path("/{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);
        if (room == null) {
            throw new ResourceNotFoundException("Room with ID '" + roomId + "' was not found.");
        }
        return Response.ok(room).build();
    }

    // POST /api/v1/rooms - create a new room
    @POST
    public Response createRoom(Room room) {
        if (room.getId() == null || room.getId().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", 400);
            error.put("error", "Bad Request");
            error.put("message", "Room 'id' field is required.");
            return Response.status(Response.Status.BAD_REQUEST).entity(error).build();
        }

        if (store.getRoom(room.getId()) != null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", 409);
            error.put("error", "Conflict");
            error.put("message", "A room with ID '" + room.getId() + "' already exists.");
            return Response.status(Response.Status.CONFLICT).entity(error).build();
        }

        store.addRoom(room);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Room created successfully.");
        response.put("room", room);

        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    // DELETE /api/v1/rooms/{roomId} - delete a room
    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = store.getRoom(roomId);

        // 404 if room doesn't exist
        if (room == null) {
            throw new ResourceNotFoundException("Room with ID '" + roomId + "' was not found.");
        }

        // Business rule: cannot delete a room that still has sensors
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }

        store.deleteRoom(roomId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Room '" + roomId + "' has been successfully decommissioned.");
        return Response.ok(response).build();
    }
}
    