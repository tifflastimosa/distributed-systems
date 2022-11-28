package Servlets;

import Configurations.PropertiesCache;
import RabbitMQ.RMQChannelFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.json.JSONObject;

@WebServlet(name = "SkierServlet", value = "/skiers/*")
public class SkierServlet extends HttpServlet {

  // name of rabbit mq - this is where we will put messages
  private final static String QUEUE_NAME = "post_requests";

  // num threads is the number of threads available in tomcat = 200
  private Integer NUM_THREADS = Integer.valueOf(PropertiesCache.getInstance().getProperty("threads"));

  // urlArray will be the array used to get parameters from the path variable
  private String[] urlArray;

  // setting up configurations
  private String HOST_NAME;
  private String USERNAME;
  private String PASSWORD;
  private String VIRTUAL_HOST;
  private String PORT;

  // get properties properties
  private void configurations() {
    this.HOST_NAME = PropertiesCache.getInstance().getProperty("hostname");
    this.USERNAME = PropertiesCache.getInstance().getProperty("username");
    this.PASSWORD = PropertiesCache.getInstance().getProperty("password");
    this.VIRTUAL_HOST = PropertiesCache.getInstance().getProperty("virtual_host");
    this.PORT = PropertiesCache.getInstance().getProperty("port");
  }

  // start the rabbit mq set up
  private Connection connection;
  private GenericObjectPool channelPool;
  private RMQChannelFactory rmqChannelFactory;

  // initialize the connection for the servlet to connect with the rabbit mq
  @Override
  public void init() throws ServletException { // creating channel pool - publishing methods
    super.init();
    configurations();
    // create the rabbit mq connection factory to set the properties of the connection
    ConnectionFactory factory = new ConnectionFactory();
//    System.out.println(HOST_NAME);
    // set the host name
    // TODO: for ec2 instance how to set up connection between servlet - rabbitmq - consumer instances
    factory.setHost(HOST_NAME);
    factory.setUsername(USERNAME);
    factory.setPassword(PASSWORD);
    try {
      // try the connection - we only want to create the connection once because it is a very expensive operation
      // create the connection with the properties set by the connection factory
      connection = factory.newConnection();
      // create the rmq channel factory that will be used for the pool
      rmqChannelFactory = new RMQChannelFactory(connection);
      // use the channel factor to create a pool of channels
      // create the shared object pool where the threads can borrow from the channel pool of
      // lightweight connections
      channelPool = new GenericObjectPool(rmqChannelFactory);
      // set the number of objects in the pool - in this case 200 b/c tomcat has 200 threads
      // number of channels - want a for loop that creates those channels and place in the pool
      channelPool.setMaxTotal(NUM_THREADS);
//      for (int i = 0; i < NUM_THREADS; i++) {
//        channelPool.addObject();
//      }
      channelPool.setMaxIdle(NUM_THREADS);
    } catch (IOException e) {
      throw new ServletException();
    } catch (Exception exception) {
      throw new ServletException();
    }
  }

  // doGet method to handle a GET request
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write("It works!");
  }

  // doPost method to handle a POST request
  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/plain");

    // get the urlPath
    String urlPath = request.getPathInfo();
    // get the json that was posted with the urlPath
    String requestJson = request.getReader().lines().collect(Collectors.joining());

    // if the urlPath does not exist, then return a 404 error
    if (urlPath == null || urlPath.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      response.getWriter().write("Missing parameters");
    }

    if (!isValidated(urlPath) || !isJsonValidated(requestJson)) {
      // if the url is not  valid or if the json is not valid, then return 404 error
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } else {
      // if the urlPath and json are valid format, incoming data (take the path, and the json object)
        // send it as payload to remote queue and then return success to the client

      // convert the url path to an array to be used to build the new payload
      convertURLPath(urlPath);
      Channel channel = null;
      try {
        String payload = buildPayload(requestJson);
//        System.out.println(payload);
        response.getWriter().write("Payload: " + payload);
        channel = (Channel) channelPool.borrowObject();
        if (channel != null) {
          channel.queueDeclare(QUEUE_NAME, false, false, false, null);
          channel.basicPublish("", QUEUE_NAME, null, payload.getBytes(StandardCharsets.UTF_8));
          response.setStatus(HttpServletResponse.SC_OK);
        }
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        if (channel != null) {
          channelPool.returnObject(channel);
        }
      }
    }
  }

  // convert the urlPath into a string array to get the variables we need to build the payload
  private void convertURLPath(String urlPath) {
    this.urlArray = urlPath.split("/");
  }

  // method gets value of the this.urlArray value (i.e. skiersId = 123)
  private Integer getPathVariableValue(int idPosition) {
    return Integer.valueOf(this.urlArray[idPosition]);
  }

  // validates the urlPath using regex and pattern matcher
  private boolean isValidated(String urlPath) {
    // /{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
    String urlPattern = "/\\d+/seasons/\\d+/days/\\d+/skiers/\\d+$";
    Pattern pattern = Pattern.compile(urlPattern, Pattern.CASE_INSENSITIVE);
    Matcher matcher = pattern.matcher(urlPath);
    return matcher.matches();
  }

  // validates the json
  private boolean isJsonValidated(String jsonRequest) {
    // declare the json object that will be used to create the string to a json
    JSONObject jsonObject;
    try {
      // create a new json objective given the string json
      jsonObject = new JSONObject(jsonRequest);
      Object timeObject = jsonObject.get("time");
      Object lifeIDObject = jsonObject.get("liftID");
      if (timeObject instanceof Integer && lifeIDObject instanceof Integer) {
        return true;
      } else {
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  // method builds the payload to put into the rabbit mq
  private String buildPayload(String jsonRequest) throws Exception {
    JSONObject jsonObject;
    try {
      // convert the JSON from POST request to JSON Object
      jsonObject = new JSONObject(jsonRequest);
      // add the values to build the payload
      jsonObject.put("resortId", getPathVariableValue(1));
      jsonObject.put("seasonId", getPathVariableValue(3));
      jsonObject.put("day", getPathVariableValue(5));
      jsonObject.put("skierId", getPathVariableValue(7));
//      System.out.println(jsonObject);
      return jsonObject.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    throw new Exception();
  }

}
