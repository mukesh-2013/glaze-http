package glaze.test.http;

import glaze.test.http.Producers.Producer;
import glaze.test.http.replay.SerializationUtil;

import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;


import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResponseBuilder
{
   public static ResponseBuilder respond(int status, Condition root)
   {
      return new ResponseBuilder(status, root);
   }

   private int status;
   private String content;
   // TODO multimap?
   private Map<String, Producer> headers;

   private static final Logger LOGGER = LoggerFactory.getLogger(ResponseBuilder.class);

   private final Condition root;

   private ResponseBuilder(int status, Condition root)
   {
      this.root = root;
      this.status = status;
      this.headers = new HashMap<String, Producer>();
   }

   public ResponseBuilder and(String key, Producer value)
   {
      this.headers.put(key, value);
      return this;
   }

   public ResponseBuilder and(String key, String value)
   {
      return and(key, Producers.id(value));
   }

   public ResponseBuilder body(String content)
   {
      this.content = content;
      return this;
   }

   public ResponseBuilder body(String content, ContentType ctype)
   {
      return body(content).contentType(ctype);
   }

   public ResponseBuilder contentType(ContentType ctype)
   {
      return and(HttpHeaders.CONTENT_TYPE, ctype.getMimeType());
   }

   public Condition getRoot()
   {
      return root;
   }

   public ResponseBuilder replay(String replayPath)
   {
      return SerializationUtil.deserialize(this, replayPath);
   }

   public ResponseBuilder status(int status)
   {
      this.status = status;
      return this;
   }

   @Override
   public String toString()
   {
      return "ResponseBuilder [status=" + status + ", content=" + content + ", headers=" + headers + "]";
   }

   public Response wrap(Response response, Request request, String data)
   {
      response.setCode(status);
      response.setContentLength(content.length());

      for (Map.Entry<String, Producer> header : headers.entrySet()) {
         response.set(header.getKey(), header.getValue().produce(request));
      }

      PrintStream body = null;
      try {
         body = response.getPrintStream();
         body.print(content);
      } catch (IOException e) {
         LOGGER.error(e.getMessage(), e);
      } finally {
         if (body != null)
            body.close();
      }
      return response;
   }
}
