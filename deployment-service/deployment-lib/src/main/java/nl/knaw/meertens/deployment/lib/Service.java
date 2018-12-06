package nl.knaw.meertens.deployment.lib;

import com.google.common.base.MoreObjects;

/**
 * @author vic
 */
public class Service {
  private String serviceId;
  private String serviceName;
  private String serviceRecipe;
  private String serviceSemantics;
  private String serviceTech;

  public Service(
    String serviceId,
    String serviceName,
    String serviceRecipe,
    String serviceSemantics,
    String serviceTech
  ) {
    this.serviceName = serviceName;
    this.serviceRecipe = serviceRecipe;
    this.serviceId = serviceId;
    this.serviceSemantics = serviceSemantics;
    this.serviceTech = serviceTech;
  }

  public Service() {}

  public String getServiceSemantics() {
    return this.serviceSemantics;
  }

  public String getServiceTechInfo() {
    return this.serviceTech;
  }

  public String getName() {
    return this.serviceName;
  }

  public String getRecipe() {
    return this.serviceRecipe;
  }

  public String getServiceId() {
    return this.serviceId;
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("serviceId", serviceId)
      .add("serviceName", serviceName)
      .add("serviceRecipe", serviceRecipe)
      .add("serviceSemantics", serviceSemantics)
      .add("serviceTech", serviceTech)
      .toString();
  }
}
