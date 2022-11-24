package DataModels;

import io.swagger.client.model.LiftRide;

public class SkiersPOJO {

  private LiftRide liftRide;

  private Integer resortID;

  private String seasonID;

  private String day;

  private Integer skierId;

  public SkiersPOJO() {
    this.liftRide = DataGenerator.jsonGenerator();
    this.resortID = DataGenerator.generateResortID();
    this.seasonID = DataGenerator.generateSeasonID();
    this.day = DataGenerator.getDay();
    this.skierId = DataGenerator.generateSkierID();
  }

  public LiftRide getLiftRide() {
    return liftRide;
  }

  public Integer getResortID() {
    return resortID;
  }

  public String getSeasonID() {
    return seasonID;
  }

  public String getDay() {
    return day;
  }

  public Integer getSkierId() {
    return skierId;
  }

  @Override
  public String toString() {
    return "SkiersPOJO{" +
        "liftRide=" + liftRide +
        ", resortID=" + resortID +
        ", seasonID='" + seasonID + '\'' +
        ", day='" + day + '\'' +
        ", skierId=" + skierId +
        '}';
  }
}
