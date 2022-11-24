package Threads;

import DataModels.SkiersPOJO;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ProducerThread extends Thread {

  private BlockingQueue<SkiersPOJO> skierPOJOBuffer;
  private AtomicInteger requiredRequests;
  private int REQ_REQUESTS = 200000;

  public ProducerThread(BlockingQueue<SkiersPOJO> skierPOJOBuffer, AtomicInteger requiredRequests) {
    this.skierPOJOBuffer = skierPOJOBuffer;
    this.requiredRequests = requiredRequests;
  }

  private void generateAndPutSkiersPOJO() {
    SkiersPOJO skierPOJO = new SkiersPOJO();
    try {
      this.skierPOJOBuffer.put(skierPOJO);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run() {
    while (this.requiredRequests.get() != REQ_REQUESTS) {
      generateAndPutSkiersPOJO();
    }
    try {
      this.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}
