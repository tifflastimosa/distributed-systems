package Threads;

import DataModels.ResponsePOJO;
import DataModels.SkiersPOJO;
import io.swagger.client.ApiClient;
import io.swagger.client.api.SkiersApi;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPool {

  private int numThreads;
  private int countDownValue;
  private int numRequests;
  private ApiClient apiClient;
  private SkiersApi skiersApi;
  private BlockingQueue<SkiersPOJO> skiersPOJOBlockingQueue;
  private BlockingQueue<ResponsePOJO> responsePOJOBlockingQueue;
  private AtomicLong threadTime;
  private AtomicInteger totalUnsuccessfulRequests;
  private AtomicInteger totalCount;
  private ExecutorService threadPool;
  private CountDownLatch countDownLatch;

  public ThreadPool(int numThreads, int countDownValue, int numRequests,
      ApiClient apiClient, SkiersApi skiersApi,
      BlockingQueue<SkiersPOJO> skiersPOJOBlockingQueue,
      BlockingQueue<ResponsePOJO> responsePOJOBlockingQueue,
      AtomicLong threadTime, AtomicInteger totalUnsuccessfulRequests,
      AtomicInteger totalCount) {
    this.numThreads = numThreads;
    this.countDownValue = countDownValue;
    this.numRequests = numRequests;
    this.apiClient = apiClient;
    this.skiersApi = skiersApi;
    this.skiersPOJOBlockingQueue = skiersPOJOBlockingQueue;
    this.responsePOJOBlockingQueue = responsePOJOBlockingQueue;
    this.threadTime = threadTime;
    this.totalUnsuccessfulRequests = totalUnsuccessfulRequests;
    this.totalCount = totalCount;
  }

  public void executeTask() {
//    System.out.println("Create Thread Pool");
    this.threadPool = Executors.newFixedThreadPool(this.numThreads);
    this.countDownLatch = new CountDownLatch(this.countDownValue);
//    System.out.println("Thread Pool Executing Tasks");
    for (int i = 0; i < this.numThreads; i++) {
      threadPool.execute(new ConsumerThread(skiersPOJOBlockingQueue, responsePOJOBlockingQueue, apiClient, skiersApi, totalCount,
          totalUnsuccessfulRequests, threadTime, numRequests, countDownLatch));
    }
  }

  public void countDownAwait() throws InterruptedException {
    this.countDownLatch.await();
  }

  public void threadPoolShutDown() {
    this.threadPool.shutdown();
  }

  public void stopThreadPool() {
    try {
      if (!this.threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
        this.threadPool.shutdownNow();
        if (!this.threadPool.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      this.threadPool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

}
