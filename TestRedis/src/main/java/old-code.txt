import Configurations.PropertiesCache;
import Model.Lift;
import Model.Student;
import com.google.gson.Gson;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisURI;
import java.util.LinkedList;
import java.util.List;
import javax.sql.PooledConnection;
import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.Document;
import redis.clients.jedis.search.Query;
import redis.clients.jedis.search.Schema;
import redis.clients.jedis.search.SearchResult;

// https://app.redislabs.com/#/login

public class Main {

  public static void main(String[] args) {

    String redisUsername = PropertiesCache.getInstance().getProperty("redis-username");
    String redisPassword = PropertiesCache.getInstance().getProperty("redis-password");

    String hostname = PropertiesCache.getInstance().getProperty("hostname");
    String password = PropertiesCache.getInstance().getProperty("password");
    Integer port = Integer.valueOf(PropertiesCache.getInstance().getProperty("port"));

    // CONNECT TO REDIS w/ URI
    // Jedis jedis = new Jedis("redis://" + redisUsername + ":" + redisPassword + "@redis-16148.c1.us-west-2-2.ec2.cloud.redislabs.com:16148");

    // CONNECT TO REDIS w/ HOSTNAME and PORT
    Jedis jedis = new Jedis(hostname, port);
//    jedis.auth(password);

    // pass this in into each consumer thread
    Connection connection = jedis.getConnection();
//    Jedis jedis = new Jedis("redis-16148.c1.us-west-2-2.ec2.cloud.redislabs.com", 16148);
//    jedis.auth(redisUsername, redisUsername);
//    jedis.hset("hello#1", "there", "123"); // this way might be marginally faster
    jedis.hset("hello#1", "okay", "there");
      // key - skierId
      // a set - track unique visits
//    System.out.println(jedis.hgetAll("hello#1"));
//    UnifiedJedis client = new UnifiedJedis(connection);
//    final Gson gson = new Gson();

    // SOLUTION 1
    // client.sadd(skierId, json representation of its lift object) // creates a new set if one doesn't exist
    // trade-off: grab all th visits with skier id
      // Q1: query - processing logic might be tricky
      // iterate through set - serialize, de-serialize, maintain a separate set that keeps track of day ids
    // 12389342 : { day: #, season: #, lift: #, time: #, resort: #, vertical: # }
    // client doesn't have to have specialized knowledge

    // SOLUTION 2
    // another approach - using additional space in redis - speed, simplicity in querying
    // flatten out data - off load some of the nested data structures
    // coming up with little data models
    // using it as a general data store
    // more keys
    // key specific to Q1
      // skierId:1234-resortId:09823
        // value: day_ids
    // key specific to Q2
      // another data model
      // might be a little more complicated
    // might be a little slower b/c doing 4x the number of writes
    // approach: build the key -> return


//    Student maya = new Student("Maya", "Jayavant");
//    client.jsonSet("student:111", gson.toJson(maya)); // associates a single json object with a key
//    System.out.println(client.jsonGet("student:112"));
//    client.jsonSet("liftId:1234", gson.toJson(new Lift(1, 12, 123, 1234, 12345, 123456)));
//    Schema schema = new Schema().addTextField("$.firstName", 1.0).addTextField("$" + ".lastName", 1.0);

//    IndexDefinition rule = new IndexDefinition(IndexDefinition.Type.JSON)
//        .setPrefixes(new String[]{"student:"});

//    client.ftCreate("student-index", IndexOptions.defaultOptions().setDefinition(rule), schema);
//    Query q = new Query("@\\$\\" + ".firstName:maya*");
//    SearchResult mayaSearch = client.ftSearch("student-index", q);
//    List<Document> docs = mayaSearch.getDocuments();
//    for (Document doc : docs) {
//      System.out.println(doc);
//    }

  }

}
