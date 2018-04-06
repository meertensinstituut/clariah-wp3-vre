/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nl.knaw.meertens.deployment.lib;

/**
 *
 * @author vic
 */
public class Service {
    private String serviceId; 
    private String serviceName;
    private String serviceRecipe;
    private String serviceSemantics;
    private String serviceTech;
    private Boolean isValidService = false;
    
    public Service() {
        this("0", "UCTO", "CLAM", "<cmdi></cmdi>", "<xml></xml>", false);
    }
    
    public Service(String serviceId, String serviceName, String serviceRecipe, String serviceSemantics, String serviceTech, Boolean isValidService) {
        this.isValidService = isValidService;
        this.serviceName = serviceName;
        this.serviceRecipe = serviceRecipe;
        this.serviceId = serviceId;
        this.serviceSemantics = serviceSemantics;
        this.serviceTech = serviceTech;
    }
    
    public String getServiceSymantics() {
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
    
    public Boolean isValid() {
        return this.isValidService;
    }
    
    @Override
    public String toString() {
        return this.getName();
    }
}
