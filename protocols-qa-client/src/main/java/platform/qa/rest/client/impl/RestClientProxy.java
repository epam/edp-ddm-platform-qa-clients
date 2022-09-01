package platform.qa.rest.client.impl;

import platform.qa.entities.Service;

public class RestClientProxy {
   private Service service;

   public RestClientProxy(Service service) {
       this.service = service;
   }

   public RestClientImpl positiveRequest() {
       return new RestClientImpl(service.getUrl(), service.getUser().getToken());
   }

   public RestClientImpl negativeRequest() {
       return new RestClientImpl(service.getUrl(), null);
   }
}
