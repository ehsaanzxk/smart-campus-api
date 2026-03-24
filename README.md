Smart Campus API
5COSC022W Client-Server Architectures Coursework
Student: Ehsaan Zakriya
Student ID: W2115831

Overview
For this coursework I built a REST API in Java using JAX-RS and Jersey. 
The API manages rooms and IoT sensors across a university campus. 
You can: 
- create rooms
- add sensors to them
- log sensor readings
and the API handles all the error cases e.g. like trying to delete a room that still has sensors in it.
The server runs using Grizzly which is an embedded HTTP server so you don't need to install Tomcat or anything extra. 
All data is stored in memory using HashMaps.


How to Build and Run

1. Make sure Java 17 and Maven are installed on your device
2. Clone the repository from GitHub
3. Open the project in NetBeans 18
4. Right click project and click Clean and Build
5. Right click the project and Run
6. The server starts at http://localhost:8080
7. All endpoints are under http://localhost:8080/api/v1

API Endpoints

Method | URL | Description  


GET | /api/v1 | Discovery endpoint - returns API info 
GET | /api/v1/rooms | Gets all rooms 
GET | /api/v1/sensors/{id}/readings | Gets all readings for a sensor 
GET | /api/v1/rooms/{id} | Get a specific room by ID 
GET | /api/v1/sensors | Gets all of the sensors 
GET | /api/v1/sensors?type=CO2 | Filters the sensors by type 

DELETE | /api/v1/rooms/{id} | Deletes a room 

POST | /api/v1/sensors | Registers a new sensor 
POST | /api/v1/rooms | Createse a new room 
POST | /api/v1/sensors/{id}/readings | Adds a new reading 



 Sample curl Commands

 1. Get all rooms

bash
curl -X GET http://localhost:8080/api/v1/rooms

2. Create a new room

bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d 

'{
    "id": 
"HALL-001", 
"name": 
"Main Hall", 
"capacity": 200
}'


 3. Register a sensor linked to a room

bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d 
'{
"id": "TEMP-002", 
"type": "Temperature", 
"status": "ACTIVE", 
"currentValue": 21.0, 
"roomId": "LIB-301"
}'

 4. Post a reading to a sensor

bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value": 23.7}'


5. Filter sensors by type

bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"

6. Try deleting a room that still has sensors (triggers 409 error)

bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301

7. Try adding a sensor with a fake room ID (triggers 422 error)
bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d 
'{
"id": "TEMP-999", 
"type": "Temperature", 
"status": "ACTIVE", 
"currentValue": 0.0, 
"roomId": "FAKE-999"
}'



Report 

1.1 - JAX-RS Resource Lifecycle

By default JAX-RS creates a brand new instance of each resource class every time a request comes in. 
So if 10 people send requests at the same time, 10 separate objects get created and thrown away. 
This means I cannot store any data inside the resource classes themselves because it would just disappear after each request.
To fix this I created a DataStore class that uses the singleton pattern and there is only ever one instance of it and it lives for the whole time the server is running. 
I used ConcurrentHashMap instead of a normal HashMap because multiple requests can come in at the same time and ConcurrentHashMap handles that safely without data getting corrupted.

1.2 - HATEOAS
HATEOAS means the API includes links to other resources in its responses. 
So when you hit the discovery endpoint at /api/v1 it gives you back links to /api/v1/rooms and /api/v1/sensors. 
This means a developer doesn't need to read through loads of documentation to find out what URLs exist and they can just start at /api/v1 and follow the links.
If the URLs ever change, the client automatically gets the new ones from the response rather than having them hardcoded somewhere that needs updating.

2.1 - Returning IDs vs Full Objects
If I only returned IDs in the rooms list, the client would have to make a separate GET request for every single room to get its details. 
for example, Iif there are 100 rooms, that is 100 extra requests which is really slow. 
Returning full objects means one request gives the client everything it needs straight away. 
The downside is the response is bigger but since room objects are small this is worth it.

2.2 - Is DELETE Idempotent
Yes it is. The first time you DELETE a room it gets removed and you get a 200 response. 
If you send the exact same DELETE request again the room is already gone so you get a 404. 
The important thing is the server state is the same both times (the room does not exist). 
Idempotent means the state stays the same no matter how many times you repeat the request which is true here.

3.1 - @Consumes Annotation
The @Consumes(MediaType.APPLICATION_JSON) annotation tells JAX-RS this method only accepts JSON. 
If a client sends the data as text/plain or application/xml instead, JAX-RS will automatically reject it before the method even runs and send back a 415 Unsupported Media Type error. 
This saves me from having to manually check the content type inside my code.

3.2 - @QueryParam vs Path Segment
Using ?type=CO2 as a query parameter is better than putting it in the URL path like /sensors/type/CO2 because the type is an optional filter and not a resource. 
The actual resource is sensors. Query parameters are designed exactly for optional filtering and searching. 
Also if I wanted to filter by multiple things like type and status at the same time, query parameters make that easy (?type=CO2&status=ACTIVE) whereas path-based filtering gets really messy.

4.1 - Sub-Resource Locator Pattern
Instead of putting all the reading endpoints inside SensorResource and making one massive class, I created a separate SensorReadingResource class just for readings. 
The SensorResource class has a locator method with no HTTP annotation that returns an instance of SensorReadingResource when a request comes in for /readings. 
This keeps the code organised and each class focused on one thing. 
If the readings logic needs changing I only touch SensorReadingResource and don't risk breaking anything in SensorResource.

5.2 - Why 422 Instead of 404
404 means the URL you requested does not exist on the server. 
But when I try to POST a sensor with a roomId that does not exist, the URL /api/v1/sensors definitely exists and was found. 
The problem is inside the JSON body - the roomId value refers to something that does not exist. 
422 Unprocessable Entity is more accurate because it means the request format is fine but the content has a logical problem. 
It gives the developer a much clearer signal about what went wrong.

5.4 - Risks of Exposing Stack Traces
If the API returned raw Java stack traces to users it would expose a lot of sensitive information. 
Attackers could see exactly which libraries and versions the app uses and look up known vulnerabilities for those versions. 
They could also see the internal package structure and class names which helps them understand how the app works and find weak points. 
My GlobalExceptionMapper catches every unhandled error and returns a simple generic 500 message so none of that internal information ever reaches the client.

5.5 - Why Use Filters for Logging
If I put Logger.info() statements inside every single resource method then the logging code would be duplicated everywhere and if I ever needed to change the logging format I would have to update every method individually. 
Using a JAX-RS filter means the logging happens automatically for every single request and response in one place. 
It also means if I add new endpoints, they automatically get logged without me having to remember to add logging code to them.