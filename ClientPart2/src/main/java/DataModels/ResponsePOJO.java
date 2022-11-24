package DataModels;

public class ResponsePOJO {
  // start time
  private long startTime;
  // end time
  private long endTime;
  // latency calculation
  private long latencyCalculation;
  // request type
  // api stats - endpoint stats, and the operation
  private String requestType;
  // response code
  private int responseCode;

  public ResponsePOJO(long startTime, long endTime, long latencyCalculation,
      String requestType, int responseCode) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.latencyCalculation = latencyCalculation;
    this.requestType = requestType;
    this.responseCode = responseCode;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public long getLatencyCalculation() {
    return latencyCalculation;
  }

  public void setLatencyCalculation(long startTime, long endTime) {
    this.latencyCalculation = endTime - startTime;
  }

  public String getRequestType() {
    return requestType;
  }

  public void setRequestType(String requestType) {
    this.requestType = requestType;
  }

  public int getResponseCode() {
    return responseCode;
  }

  public void setResponseCode(int responseCode) {
    this.responseCode = responseCode;
  }

  @Override
  public String toString() {
    return startTime + " " + endTime + " " + latencyCalculation + " "  + requestType + " "  + responseCode;
  }

}
