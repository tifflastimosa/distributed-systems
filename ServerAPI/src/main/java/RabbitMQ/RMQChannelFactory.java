package RabbitMQ;

import Configurations.PropertiesCache;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;


public class RMQChannelFactory extends BasePooledObjectFactory<Channel> {

  private final Connection connection;
  private int count;

  public RMQChannelFactory(Connection connection) {
    this.connection = connection;
    count = 0;
  }

  @Override
  synchronized public Channel create() throws Exception {
    if (count > Integer.valueOf(PropertiesCache.getInstance().getProperty("threads"))) {
      return null;
    } else {
      count++;
      Channel channel = connection.createChannel();
      return channel;
    }
  }

  @Override
  public PooledObject<Channel> wrap(Channel channel) {
    return new DefaultPooledObject<>(channel);
  }

  public int getChannelCount() {
    return count;
  }

  @Override
  public void destroyObject(PooledObject<Channel> p) throws Exception {
    Channel channel = (Channel) p;
    channel.close();
  }
}
