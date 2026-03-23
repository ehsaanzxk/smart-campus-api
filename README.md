 Smart Campus API
 5COSC022W Client-Server Architectures CWRK 
 Ehsaan Zakriya

A RESTful API built with JAX-RS and Jersey for managing rooms and IoT sensors across the university campus.

Overview
This API allows campus staff to manage rooms, register sensors in those rooms, and log the sensor readings over time. It is built using Java, JAX-RS, Jersey and an embedded Grizzly server.

Instructions:

1. Make sure you have Java 17 and Maven installed
2. Clone repository
3. Open the project in NetBeans
4. Right-click the project and click Clean and Build
5. Right-click the project and click Run
6. The server starts at http://localhost:8080

## API Endpoints

| Method | URL | Description 

-  GET | /discovery | API info and links |
-  GET | /rooms | Get all rooms |
- POST | /rooms | Create a room |
-  GET | /rooms/{id} | Get a specific room |
-  DELETE | /rooms/{id} | Delete a room |
-  GET | /sensors | Get all sensors |
-  GET | /sensors?type=CO2 | Filter sensors by type |
-  POST | /sensors | Register a sensor |
-  GET | /sensors/{id}/readings | Get sensor readings |
-  POST | /sensors/{id}/readings | Add a reading |

