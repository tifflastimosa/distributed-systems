package DataModels;

import io.swagger.client.model.LiftRide;
import java.security.SecureRandom;
import java.time.LocalDate;

public class DataGenerator {

  private static final int LOWERBOUND = 1;
  protected static final int DAY = 1;

  public DataGenerator() {
  }

  public static Integer generateResortID() {return idGenerated(10);}

  public static String generateSeasonID() {
    return String.valueOf(LocalDate.now().getYear());
  }

  public static String getDay() {
    return String.valueOf(LOWERBOUND);
  }

  public static Integer generateSkierID() { return idGenerated(100000); }

  public static LiftRide jsonGenerator() {
    Integer liftID = idGenerated(40);
    Integer time = idGenerated(360);
    // "{ \"time\": 217, \"liftID\": 21 }";
    LiftRide liftRide = new LiftRide();
    liftRide.setLiftID(liftID);
    liftRide.setTime(time);
    return liftRide;
  }

  public static Integer idGenerated(int upperBound) {
    SecureRandom secureRandom = new SecureRandom();
    return secureRandom.nextInt(upperBound - LOWERBOUND) + LOWERBOUND;
  }

}
