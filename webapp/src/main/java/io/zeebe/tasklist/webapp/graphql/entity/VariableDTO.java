package io.zeebe.tasklist.webapp.graphql.entity;

public class VariableDTO {

  private String name;
  private String value;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    VariableDTO variable = (VariableDTO) o;

    if (name != null ? !name.equals(variable.name) : variable.name != null)
      return false;
    return value != null ? value.equals(variable.value) : variable.value == null;

  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (value != null ? value.hashCode() : 0);
    return result;
  }
}
