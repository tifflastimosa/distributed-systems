import Configurations.PropertiesCache;
import Model.Lift;
import Model.Student;
import com.google.gson.Gson;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnection;
import com.lambdaworks.redis.RedisURI;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.sql.PooledConnection;
import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
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

    // CONNECT TO REDIS w/ HOSTNAME and PORT
    Jedis jedis = new Jedis(hostname, port);

    // pass this in into each consumer thread
    Connection connection = jedis.getConnection();
    jedis.hset("hello#1", "okay", "there");
    // key - skierId
    // a set - track unique visits
    System.out.println(jedis.hgetAll("hello#1"));


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

  }

}
