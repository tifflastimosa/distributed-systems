package Threads;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.json.JSONObject;

public class ConsumerThread extends Thread {

  // need the channel to connect to rabbit mq
  private Channel channel;

  private ConcurrentHashMap<Integer, ArrayList<JSONObject>> skiersStorage;

  private final String QUEUE_NAME;

  public ConsumerThread(Connection connection, ConcurrentHashMap skiersStorage, String QUEUE_NAME) throws IOException {
    // create the channel
    this.channel = connection.createChannel();
    this.skiersStorage = skiersStorage;
    this.QUEUE_NAME = QUEUE_NAME;
    this.channel.queueDeclare(this.QUEUE_NAME, false, false, false, null);
  }

  @Override
  public void run() {
    // take the message from the queue
    // unravel the message - need to convert the string to the a json object
    // take the skier id - use it as a key, and the json object as a value
    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
      String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
      // convert the String to JSON object
      JSONObject jsonObject = new JSONObject(message);
      // the case if the concurrent hashmap does not contain the skier
      Integer skierId = (Integer) jsonObject.get("skierId");
      if (this.skiersStorage.containsKey(skierId)) {
        skiersStorage.get(skierId).add(jsonObject);
      } else {
        skiersStorage.put(skierId, new ArrayList<>());
        skiersStorage.get(skierId).add(jsonObject);
      }
      channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
      // the case if the concurrent hashmap contains the skier
    };
    try {
      channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> { });
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
