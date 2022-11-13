import Configurations.PropertiesCache;
import Threads.ConsumerThread;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONObject;


public class Main {

  public static void main(String[] args) throws IOException, TimeoutException {

    final int NUM_THREADS = 200;

    final String QUEUE_NAME = "post_requests";

    ConnectionFactory factory = new ConnectionFactory();
    factory.setUsername(PropertiesCache.getInstance().getProperty("username"));
    factory.setPassword(PropertiesCache.getInstance().getProperty("password"));
    factory.setHost(PropertiesCache.getInstance().getProperty("hostname"));
    Connection connection = factory.newConnection();

    // concurrent hashmap - shared resource
    ConcurrentHashMap<Integer, ArrayList<JSONObject>> skiersStorage = new ConcurrentHashMap<>();

    ExecutorService threadPool = Executors.newFixedThreadPool(NUM_THREADS);
    for (int i = 0; i < NUM_THREADS; i++) {
      threadPool.execute(new ConsumerThread(connection, skiersStorage, QUEUE_NAME));
    }
    try {
      if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
        threadPool.shutdownNow();
        if (!threadPool.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      threadPool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

}
