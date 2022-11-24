import DataModels.ResponsePOJO;
import DataModels.SkiersPOJO;
import Threads.ConsumerThread;
import Threads.ProducerThread;
import com.opencsv.CSVWriter;
import io.swagger.client.ApiClient;
import io.swagger.client.api.SkiersApi;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class Main {

  public static Long calculateMedian(ArrayList<Long> time) {
    int n = time.size();
    if (n % 2 == 1) {
      int index = (n + 1) / 2 -1;
      return time.get(index);
    } else {
      int index1 = n / 2 - 1;
      int index2 = n/ 2;
      return (time.get(index1) + time.get(index2))/2;
    }

  }

  public static Long calculateMean(ArrayList<Long> time) {
    Long total = Long.valueOf(0);
    for (Long i : time) {
      total += i;
    }
    return total / time.size();
  }

  public static void main(String[] args) throws InterruptedException {

    // Schedule 1 - 32 initial threads
    final int NUMTHREADS1 = 32;
    // schedule 2 - 168 initial threads
    final int NUMTHREADS2 = 300;
    // number of requests
    final int REQ_REQUESTS = 1000;

    // run 2nd phase
    boolean phase2Enabled = true;

    // initialize shared resources
    BlockingQueue<SkiersPOJO> skiersPOJODataBuffer = new LinkedBlockingQueue<>();
    BlockingQueue<ResponsePOJO> responsePOJOData = new LinkedBlockingQueue<>();
    // api client
    ApiClient apiClient = new ApiClient();
    // skier api
    SkiersApi skiersApi = new SkiersApi(apiClient);
    // total requests made
    AtomicInteger totalCount = new AtomicInteger(0);
    // total unsuccessful requests
    AtomicInteger totalUnsuccessfulRequests = new AtomicInteger(0);
    // total thread time - taken by adding the sum
    AtomicLong threadTime = new AtomicLong(0);

    Thread producerThread = new Thread(new ProducerThread(skiersPOJODataBuffer, totalCount));
    producerThread.start();

    long startTimeProgram = System.currentTimeMillis();

    // first countdown
    ExecutorService consumerPool1 = Executors.newFixedThreadPool(NUMTHREADS1);
    CountDownLatch firstCountdown = new CountDownLatch(1);
    for (int i = 0; i < NUMTHREADS1; i++) {
      consumerPool1.execute(new ConsumerThread(skiersPOJODataBuffer, responsePOJOData, apiClient, skiersApi, totalCount,
          totalUnsuccessfulRequests, threadTime, REQ_REQUESTS, firstCountdown));
    }
    firstCountdown.await();
    consumerPool1.shutdown();

    ExecutorService consumerPool2 = null;
    if (phase2Enabled) {
      consumerPool2 = Executors.newFixedThreadPool(NUMTHREADS2);
      CountDownLatch secondCountdown = new CountDownLatch(NUMTHREADS2);
      for (int i = 0; i < NUMTHREADS2; i++) {
        consumerPool2.execute(new ConsumerThread(skiersPOJODataBuffer, responsePOJOData, apiClient, skiersApi, totalCount,
            totalUnsuccessfulRequests, threadTime, REQ_REQUESTS, secondCountdown));
      }
      secondCountdown.await();
      consumerPool2.shutdown();
    }

    try {
      if (!consumerPool1.awaitTermination(60, TimeUnit.SECONDS)) {
        consumerPool1.shutdownNow();
        if (!consumerPool1.awaitTermination(60, TimeUnit.SECONDS))
          System.err.println("Pool did not terminate");
      }
    } catch (InterruptedException ie) {
      consumerPool1.shutdownNow();
      Thread.currentThread().interrupt();
    }

    if (phase2Enabled) {
      try {
        if (!consumerPool2.awaitTermination(60, TimeUnit.SECONDS)) {
          consumerPool2.shutdownNow();
          if (!consumerPool2.awaitTermination(60, TimeUnit.SECONDS))
            System.err.println("Pool did not terminate");
        }
      } catch (InterruptedException ie) {
        consumerPool2.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    long endTimeProgram = System.currentTimeMillis();

    long wallTime = endTimeProgram - startTimeProgram;

    List<String[]> forCSV = new ArrayList<>();
    ArrayList<Long> time = new ArrayList<>();

    for (ResponsePOJO responsePOJO : responsePOJOData) {
//      System.out.println(responsePOJO.toString());
      String[] record = responsePOJO.toString().split(" ");
      forCSV.add(record);
      time.add(responsePOJO.getLatencyCalculation());
    }

    String[] headers = {"Start Time", "End Time", "Latency", "Type", "Code"};
    try (CSVWriter writer = new CSVWriter(new FileWriter("/Users/tiffany/GitHub/cs6650-distributed-systems/Assignments/lastimosa-assignment1/ClientPart2/records.csv"))) {
      writer.writeNext(headers);
      writer.writeAll(forCSV);
    } catch (IOException e) {
      e.printStackTrace();
    }

    Collections.sort(time);
    int totalRequests = totalCount.get() + totalUnsuccessfulRequests.get();
    int count = NUMTHREADS1;
    int requests = REQ_REQUESTS * count;
    int percentile = (99 * count) / 100;

    if (phase2Enabled) {
      count = NUMTHREADS1 + NUMTHREADS2;
      requests = REQ_REQUESTS * count;
    }

    System.out.println("----------------------------Configuration----------------------------");
    System.out.println("Number of Threads: " + count);
    System.out.println("Total Requests: " + requests);

    System.out.println("-----------------------------Calculations----------------------------");
    System.out.println("Num of total requests: " + totalRequests);
    System.out.println("Num of successful requests: " + totalCount.get());
    System.out.println("Num of unsuccessful requests: " + totalUnsuccessfulRequests.get());
    System.out.println("Wall Time: " + String.valueOf(wallTime) + " ms");
    System.out.println("Throughput: " + String.valueOf(totalCount.get()/(wallTime * 0.001)) + " requests per second");

    System.out.println("---------------------------------Stats-------------------------------");
    System.out.println("Mean: " + String.valueOf(Main.calculateMean(time)) + " ms");
    System.out.println("Median: " + String.valueOf(Main.calculateMedian(time)) + " ms");
    System.out.println("Min Response Time: " + String.valueOf(time.get(0)) + " ms");
    System.out.println("Max Response Time: " + String.valueOf(time.get(time.size() - 1)) + " ms");
    System.out.println("99th Percentile: " + String.valueOf(time.get(percentile)+ " ms"));
    System.exit(0);
  }

}
