package Threads;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

public class ConsumerThread extends Thread {

  // need the channel to connect to rabbit mq
  private Channel channel;

  private final String QUEUE_NAME;

  // Jedis is not thread safe, thus create Jedis connection in each Consumer thread
  private Jedis jedis;

  private String key;


  public ConsumerThread(Connection connection, String host, Integer port, String QUEUE_NAME) throws IOException {
    // create the channel
    try {
      this.channel = connection.createChannel();
    } catch (Exception e) {
      e.printStackTrace();
    }
    try {
      this.jedis = new Jedis(host, port);
    } catch (Exception exception) {
      exception.printStackTrace();
    }
    this.QUEUE_NAME = QUEUE_NAME;
    this.channel.queueDeclare(this.QUEUE_NAME, false, false, false, null);
  }

  @Override
  public void run() {
    // take the message from the queue
    // unravel the message - need to convert the string to a json object
    // take the skier id - use it as a key, and the json object as a value
//    System.out.println(Thread.currentThread().getId());
    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      if (delivery != null) {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        // convert the String to JSON object
        JSONObject jsonObject = new JSONObject(message);
        // the case if the concurrent hashmap does not contain the skier
        Integer vertical = (Integer) jsonObject.get("liftID") * 10;

        String skierIDKey = "skierId:" + jsonObject.get("skierId");
        String resortIDKey = "resortId:" + jsonObject.get("resortId");
        String seasonIDKey = "seasonId:" + jsonObject.get("seasonId");
        String dayIDKey = "day:" + jsonObject.get("day");
        String timeIDKey = "time:" + jsonObject.get("time");
        String liftIDKey = "liftID:" + jsonObject.get("liftID");
        String verticalKey = "vertical" + String.valueOf(vertical);
        String UUIDKey = "UUID:" + UUID.randomUUID();

        this.key = skierIDKey + "-" + resortIDKey + "-" + seasonIDKey + "-" + dayIDKey + "-" + timeIDKey + "-" + liftIDKey + "-" + verticalKey + "-" + UUIDKey;
        if (key != null) {
          this.jedis.sadd(this.key, verticalKey);
        }
        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      }
    };
    try {
      channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
