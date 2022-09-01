package platform.qa.rest.client;

import platform.qa.entities.IEntity;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.Map;

public interface RestClient {
    <Response> Response get(String path,
                            @Nullable Map<String, String> pathParams,
                            Type type,
                            int statusCode);
    <Request extends IEntity, Response> Response post(String path,
                                                      @Nullable Map<String, String> pathParams,
                                                      Request body,
                                                      Type type,
                                                      int statusCode);
    <Request extends IEntity, Response> Response put(String path,
                                                     @Nullable Map<String, String> pathParams,
                                                     Request body,
                                                     Type type,
                                                     int statusCode);
    void delete(String path,
                Type type,
                int statusCode);
}
