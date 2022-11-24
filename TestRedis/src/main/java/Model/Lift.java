package Model;

// POJO
public class Lift {

  private String id; // this will be unique
  private int resort_id;
  private int season_id;
  private int day_id;
  private int skier_id;
  private int lift_id;
  private int time;
  private int vertical_id;

  public Lift(int resort_id, int season_id, int day_id, int skier_id, int lift_id,
      int time) {
    // UUID
    this.resort_id = resort_id;
    this.season_id = season_id;
    this.day_id = day_id;
    this.skier_id = skier_id;
    this.lift_id = lift_id;
    this.time = time;
    this.vertical_id = lift_id * 10;
  }

  public Lift() {
  }

  public String getId() {
    return id;
  }

  public int getResort_id() {
    return resort_id;
  }

  public void setResort_id(int resort_id) {
    this.resort_id = resort_id;
  }

  public int getSeason_id() {
    return season_id;
  }

  public void setSeason_id(int season_id) {
    this.season_id = season_id;
  }

  public int getDay_id() {
    return day_id;
  }

  public void setDay_id(int day_id) {
    this.day_id = day_id;
  }

  public int getSkier_id() {
    return skier_id;
  }

  public void setSkier_id(int skier_id) {
    this.skier_id = skier_id;
  }

  public int getLift_id() {
    return lift_id;
  }

  public void setLift_id(int lift_id) {
    this.lift_id = lift_id;
  }

  public int getTime() {
    return time;
  }

  public void setTime(int time) {
    this.time = time;
  }
}
