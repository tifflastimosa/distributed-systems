# Scalable Distributed System

## Goal
- The goal of this project is to create a microservice that will have high throughput, availability, and scalability.
- Design a client that will test and measure throughput of the microservice.

## Client Design
### Client Design 1
For the multithreaded client, I used a Producer-Consumer approach. The predicted throughput for this client was 5471.3 requests per second. To meet this predicted throughput, the goal was to utilize shared resources but minimally as possible and not have the ConsumerThread instantiate 1000+ objects to make a POST request. The client has two types of threads: a ProducerThread and a ConsumerThread. Both of these threads inherit from the parent class, Thread, and they implement a run task that will be used to make POST requests.

The purpose of the ProducerThread is to generate data to be utilized by the ConsumerThread to make the POST request. A data generator and data model classes were needed to make this work. The DataGenerator class is responsible for randomly generating numbers given specific parameters. The SkiersPOJO is the data model that calls the DataGenerator. Thus, when the SkiersPOJO is instantiated, the lift ride, resort id, and season id generate random values, and the day is set. All the instance variables are then used in the ConsumerThread to make the request with the writeNewLiftWithHttpInfo method.

Once the ProducerThread creates the SkiersPOJO, the ProducerThread then puts the SkiersPOJO onto the data buffer. The data buffer is a shared resource between the ProducerThread and the ConsumerThread, and to ensure thread safety, a Linked Blocking Queue was used. Other shared resources were used, such as the ApiClient and the SkiersApi. These were created to set the base path and make POST requests, so it was sufficient to instantiate in the Main thread and pass it to the ConsumerThread rather than instantiate multiple of ApiClient and SkiersApi objects for each thread. Variable totalCount and totalUnsuccessful are both atomic variables that are incremented in the consumer thread. To prevent thread interrupt, each ConsumerThread had their own counters and before the thread exits, the ConsumerThread's variables adds to the atomic variables.

The responsibility of the ConsumerThread is to make a given number of POST requests. To do this, the ConsumerThread pulls the SkiersPOJO from the shared data buffer and calls writeNewLiftWithHttpInfo. Data and errors are both handled accordingly. A while loop is used to make the given number of requests.

To make this client multithreaded, the executor framework was used to allow us to create different policies for task execution. With ExecutorService implemented, the pool size can be controlled, and execute can be called to start making the POST requests. Additionally, CountDownLatch was used to meet the requirement of creating new threads after a thread has completed its tasks.

Overall, on start up of the program, 1 ProducerThread starts to put threads in the Linked Blocking Queue. 32 ConsumerThreads are then created and their tasks begin executing. Once 1 thread finishes, 168 threads are then created and their tasks begin executing so that 200,000 POST requests are made.

### Client Design 2
In part 2 of this assignment, another data model was needed to record each thread's latency so that it could be added to another Linked Blocking Queue. The ResponsePOJO is a data model that records the start time of the response, end time of the response, the latency, the request type, and the response type.

The ConsumerThread initiates the array that will contain the ResponsePOJO. To prevent a thread from blocking the Linked Blocking Queue, an array ArrayList is initialized in each ConsumerThread, and each response is then added at the array. Once the ConsumerThread has completed all the requests, it will add all the ResponsePOJO is the shared Linked Blocking Queue.

After all threads are done making the required number of POST requests, the client will generate the .csv and calculate the mean, median, throughput, and the 99th percentile.

## Server Design
### Validation
In the doPost() method, to retrieve the data that needed to be validated and utilized to build the payload for the message queue, the HttpServletRequest parameter was used to grab the path and the JSON for validation. Regex was used and followed the path variable pattern, "/\\d+/seasons/\\d+/ days/\\d+/skiers/\\d+$, and validated by using the Pattern and Matcher class. The JSON was validated by using the org.json package. With the package, the keys were retrieved to confirm if they were integers.

Once the path and JSON were validated, the payload was built. To build the payload, the path was split into a String array using the “/” as a delimiter. With the String array, the indices were used to create key-value pairs for the JSON. The resulting payload was built as a JSON, including the following keys: resort id, season id, day, skier id, time, and lift id. To publish the payload to the RabbitMQ (“message queue”), the payload was converted from a JSON to a String object.

### Producer Design
The class “ServerAPI” (“servlet”) is the servlet used to process POST requests and the producer of the RabbitMQ (“message queue”) to implement an asynchronous system.

#### A. Establishing the Connection
A trade-off of implementing an asynchronous system is establishing a connection to the message queue. Establishing a connection to the message queue is an expensive operation because it makes a TCP handshake. In the SkierServlet class, the init method is overridden to initialize the connection to the message queue.

#### B. Putting the Payload to the Message Queue
Once the connection has been initialized and message queue configurations have been set, the RMQChannelFactory object is instantiated with the connection as the parameter. The RMQChannelFactory inherits from the BasePooledObjectFactory, an Apache class. The factory will be used to create a pool of Channels. As mentioned above, creating a connection to the message queue is an expensive operation. The solution was to create a global channel pool that the Tomcat threads used to publish methods onto the message queue.

The Apache class used to create an object pool of Channels is the GenericaObjectPool. This allows the threads to borrow Channel objects to make the connection to the message queue and publish the payload. With the GenericObjectPool, the number of Channels can be set, borrowed, etc.

In the doPost method, after validation and creation of the payload, a thread will borrow (using the borrowObject() method) a Channel from the Channel pool created by the GenericObjectPool, then use the Channel to connect to the message queue, and publish the payload. Once the message has been published to the queue, the Channel is returned to the channel pool (using the returnObject() method) for another thread to use. In this approach, the channel-per-thread is used since Channels are not thread-safe.

### C. Consumer Design
The channel-per-thread model was also used on the Consumer. In the Consumer, there is a set number of threads that a user can change. Like the Producer, a connection is made. However, the key difference is that a pool was not created. Rather, the connection was passed into the Consumer class, where a Channel is created. Thus, the number of threads set is the number of Channels created. In the ConsumerThread class, the basicConsume() method is called to retrieve or consume the message from the message queue and acknowledges it by using the basicAck() method in the DeliverCallback method.

In the DeliverCallback methods, the message retrieved from the basicConsume() method call is converted to a JSONObject. The JSONObject is then put into a ConcurrentHashMap (“hashmap”) to store the values. To store the JSONObject in the hashmap, the skier id is used as the key, and the value is an array list to store other JSONObjects with the same key.

### D. Questions, Addressing the Network Latency
After testing different configurations, the optimal number of threads is 168 threads. When attempting to run 200 threads, 300 threads, or 400 threads, it resulted in socket errors due to being hampered by the network latency. However, with 168 threads, there were either no unsuccessful requests or the number of unsuccessful requests was very small.

 Testing different configurations revealed that the number of threads to keep the queue as close to zero as possible is less than 500. The number of consumer threads equal to the number of producers or slightly higher than the number of producers is sufficient to keep the queue size as close to zero as possible. When the consumer threads were above 500, the queue size started to grow. In one test, it grew to 800 messages. From this observation, I reasoned that the memory space of the RabbitMQ was insufficient for the 1000 Channels caused by the consumer thread.
 
Both the RabbitMQ instance and the Consumer instance are on the free tier. Thus, the memory size is not ideal when the Producer publishes many messages, especially when it is load balanced.

### E. Throughput Observation, Tune the System
Given the below, throughput somewhat improved in that I received fewer socket exceptions compared to the last assignment; however, the throughput value did not improve much.

In an attempt to improve the system, the RabbitMQ instance was changed to t3.large to increase the memory for the 1000 threads created by the Consumer. However, this did not improve much because of the network speed.

Analyzing the single instance and 2 instance servlet, the throughput did not improve. The difference between the throughputs was very minuscule, and the cause for not seeing an improvement in the throughput is due to the network latency. It is important to note that the throughput is very close to assignment 1, which experienced low throughput because of network latency. Thus, this shows how the network can be a factor in affecting throughput. (Screenshot of network quality below.)

## Database/Cache Design
Three databases were considered: MySQL, MongoDB, DynamoDB, and Redis. MySQL was considered because it is robust, available, and open sourced. However, it lacks flexibility. MongoDB was then considered because it is document based and it is flexible should there be any changes to a data model. It can also handle complex queries and it is scalable. DynamoDB was also considered because it was fast.

On the other hand, Redis is an in-memory database and it much faster in comparison to MySQL and MongoDB. Moreover, it is easy to set-up, deploy on an ec2 instance, and it offers the Jedis dependency to create connections and write a Java program to write to the Redis database. Thus, I chose Redis to try a new database and observe how fast it was.

### Redis Schema
When designing the database schema for Redis, it became apparent that there many trade-offs because it is a key-value data store.
#### A. Potential Schema 1 - Sets
| Key | Value    | 
| :---:   | :---: |
| skierId:12389342- UUID:1234-5678-9012-3456 | { “dayId”: 1, “seasonId”: 123, “resortId”: 123, “time”: 12, “lift”: 20, “vertical” 200 } | 

**Trade-off:** In this solution, it applies the idea of a document database. One benefit of this approach, is that data can be quickly written to the Redis database and prevent data from being overwritten. It would allow the proposed queries, but the values are nested. To query with this schema, the user would have to get the desired skierId and then query each value of the skier. In the worst-case scenario, there would be a plethora of the desired skierId (i.e.100,000 entries) and the user would have to query each key. The GET requests may take much longer in comparison to POST requests and would not be ideal for a web application.
#### B. Potential Schema 2 - Sets
| Key | Value    | 
| :---:   | :---: |
| skierId:12389342- UUID:1234-5678-9012-3456 | { “dayId”: 1, “seasonId”: 123, “resortId”: 123, “time”: [ 12, 35, 21 ], “lift”: [ 20, 10, 20 ], “vertical” [ 200, 100, 200 ] } | 

 **Trade-off:** This solution also applies the idea of a document database. The difference is that a PUT operation would have to be implemented in the Java Consumer program. Thus, the Java program would have a POST operation if the skierId did not exist, but a PUT operation if the skiderId did exist. The skierId would have to be found and access the nested times, lifts, and verticals, which are stored in arrays. It could potentially handle the queries, but it would be much more complicated because the user would have to query the nested arrays.
 #### C. Potential Schema 3 - Hashes key
| Key | Value | Attribute|
| :---:   | :---: | :---: |
| skierId:12389342-UUID:1234-5678- 9012-3456 | “dayId”   | 1   |
| skierId:12389342-UUID:1234-5678- 9012-3456 | “seasonId”   | 123   |
| skierId:12389342-UUID:1234-5678- 9012-3456 | “resortId”   | 123   |
| skierId:12389342-UUID:1234-5678- 9012-3456 | “time”   | 12   |
| skierId:12389342-UUID:1234-5678- 9012-3456 | "lift"   | 20   |
| skierId:12389342-UUID:1234-5678- 9012-3456 | "vertical"   | 200   |
 
**Trade-off:** This schema uses the hashes data type in Redis. This would not slow down the POST operation, however, the user is presented with complex queries because they would have to get the desired skierId and then grab each attribute. It is very similar to the previous options because it works with nested data to perform the queries.
#### D. Schema 4 - Sets
| Key | Value    | 
| :---:   | :---: |
|skierId:12389342-resortId:123- seasonId:123-dayId:1-time:12-lift:27- vertical:270-UUID:1234-5678-9012-3456 | {"vertical":270} | 

**Trade-off:** Schema 4 uses sets. Moreover, rather than storing all the data as a value, the data is stored in the key. This makes it easy to do a POST operation and would make it easier to query by using a delimiter. The caveat to this schema is that the user would have to define each query they desire. It is up to the developer to anticipate of what would be queried or to work closely with their team to determine what queries would be needed.

For the schema, I chose to go with option d) Schema 4 because it would be easier to query since the data to be queried will not be nested Moreover, it will be faster to write to the Redis database.

## Deployment Topologies | Instance Types

 If you would like to view the Google Sheet of the topologies and instance types, please see the following Google Sheet link: [results](https://docs.google.com/spreadsheets/d/1MslOg1dNi8RPU39Hcz62SBnPO4CXvKimWL9NAlCqbRg/edit?usp=sharing). When reviewing the report, please refer to the color legend for additional information regarding the topologies and instance types.

## Design Diagrams
![](https://github.com/tifflastimosa/distributed-systems/blob/main/Screenshots/Step-1.jpeg)
