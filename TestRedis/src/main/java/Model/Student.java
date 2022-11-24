package Model;

import java.util.ArrayList;

public class Student {

  private String firstName;
  private ArrayList<String> lastName;

  public Student(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = new ArrayList<>();
  }

  public String getFirstName() {
    return firstName;
  }

  public ArrayList<String> getLastName() {
    return lastName;
  }
}
