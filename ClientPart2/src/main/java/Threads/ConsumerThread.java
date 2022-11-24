package Threads;

import DataModels.ResponsePOJO;
import DataModels.SkiersPOJO;
import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.ApiResponse;
import io.swagger.client.api.SkiersApi;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ConsumerThread extends Thread {
  // shared states | constants
  private BlockingQueue<SkiersPOJO> skierPOJOBuffer;
  private BlockingQueue<ResponsePOJO> responsePOJO;
  // constant for max attempts - passed by the parameter
  private final int MAXATTEMPTS = 5;
  private int REQ_SUCCESSFUL_REQ;
  private ApiClient apiClient;
  private SkiersApi skiersApi;
  // countdown latch - latch is used to execute another task after 1 thread is completed
  private CountDownLatch countDownLatch;

  // instance variables
  private AtomicInteger totalCount;
  private AtomicInteger totalUnsuccessfulRequests;
  private AtomicLong threadTime;
  private int count;
  private int numUnSuccessfulRequests;
  private ArrayList<ResponsePOJO> records;


  public ConsumerThread(BlockingQueue<SkiersPOJO> skierPOJOBuffer,
      BlockingQueue<ResponsePOJO> responsePOJO,
      ApiClient apiClient, SkiersApi skiersApi,
      AtomicInteger totalCount,
      AtomicInteger totalUnsuccessfulRequests,
      AtomicLong threadTime,
      int REQ_SUCCESSFUL_REQ,
      CountDownLatch countDownLatch) {
    this.skierPOJOBuffer = skierPOJOBuffer;
    this.apiClient = apiClient;
    this.skiersApi = skiersApi;
    this.totalCount = totalCount;
    this.totalUnsuccessfulRequests = totalUnsuccessfulRequests;
    this.REQ_SUCCESSFUL_REQ = REQ_SUCCESSFUL_REQ;
    this.countDownLatch = countDownLatch;
//    apiClient.setBasePath("http://localhost:8080/ServerAPI/");
//    apiClient.setBasePath("http://localhost:8080");
    apiClient.setBasePath("http://servlet-load-balancer-d76b44bd4bcf6144.elb.us-west-2.amazonaws.com:8080/ServerAPI_war/");
    this.count = 0;
    this.numUnSuccessfulRequests = 0;
    this.threadTime = threadTime;
    this.responsePOJO = responsePOJO;
    this.records = new ArrayList<>();
  }


  @Override
  public void run() {
    // what has to happen in this task? this thread is responsible for executing 1000 successful POST requests
    long startingTimeThread = System.currentTimeMillis();
    ApiResponse<Void> response;
//    System.out.println("Thread start: " + this.getName());
    while (this.count != this.REQ_SUCCESSFUL_REQ) {
      try {
        if (skierPOJOBuffer.isEmpty()) {
          continue;
        }
        SkiersPOJO skier = skierPOJOBuffer.take();
        long startingTimeReq = System.currentTimeMillis();
        response = skiersApi.writeNewLiftRideWithHttpInfo(skier.getLiftRide(),
            skier.getResortID(), skier.getSeasonID(), skier.getDay(), skier.getSkierId());
        // if the response is 200 or 201 response, then increment count
        long endResponseTime = System.currentTimeMillis();
        if (isValid(response)) {
          validResponse(response, startingTimeReq, endResponseTime);
        } else { // if response is error
          int attempts = 0;
          while (attempts != this.MAXATTEMPTS) {
            startingTimeReq = System.currentTimeMillis();
            response = skiersApi.writeNewLiftRideWithHttpInfo(skier.getLiftRide(),
                skier.getResortID(), skier.getSeasonID(), skier.getDay(), skier.getSkierId());
            long endResponseTimeReAttempt = System.currentTimeMillis();
            if (isValid(response)) {
              validResponse(response, startingTimeReq, endResponseTime);
              break;
            } else {
              attempts++;
              this.records.add(new ResponsePOJO(startingTimeReq,  endResponseTimeReAttempt,
                  endResponseTimeReAttempt - startingTimeReq, "POST",
                                  response.getStatusCode()));
            }
          }
          if (response.getStatusCode() != 200 || response.getStatusCode() != 201 ) {
            this.numUnSuccessfulRequests++;
          }
        }
      } catch (ApiException e) {
//        System.out.println("ApiException - Response: " + e.getCode());
        this.numUnSuccessfulRequests++;
        count++;
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    long endingTimeThread = System.currentTimeMillis();
    this.totalCount.addAndGet(this.count);
    this.totalUnsuccessfulRequests.addAndGet(this.numUnSuccessfulRequests);
    this.threadTime.addAndGet(endingTimeThread - startingTimeThread);
    this.responsePOJO.addAll(this.records);
    // at the end of the task want to countDown and launch more threads
    countDownLatch.countDown();
//    System.out.println("Thread shutdown: " + this.getName());
    try {
      this.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private boolean isValid(ApiResponse<Void> response) {
    if (response.getStatusCode() == 200 || response.getStatusCode() == 201) {
      return true;
    }
    return false;
  }

  private void validResponse(ApiResponse<Void> response, long startTime, long endTime) {
    count++;
    this.records.add(new ResponsePOJO(startTime, endTime, endTime - startTime, "POST", response.getStatusCode()));
  }
}
