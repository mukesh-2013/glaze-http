package glaze.client.async;

import glaze.GlazeException;
import glaze.client.BaseClient;
import glaze.client.Client;
import glaze.client.Response;
import glaze.client.handlers.ErrorHandler;
import glaze.client.interceptors.PreemptiveAuthorizer;
import glaze.spi.Registry;
import glaze.util.RequestUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Future;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthSchemeFactory;
import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.nio.client.AbstractHttpAsyncClient;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.conn.scheme.AsyncScheme;
import org.apache.http.nio.conn.ssl.SSLLayeringStrategy;
import org.apache.http.nio.entity.EntityAsyncContentProducer;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.protocol.BasicAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.IOReactorStatus;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParamBean;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.VersionInfo;

import com.google.common.base.Preconditions;

/**
 * Default implementation of {@link AsyncClient}.
 * 
 */
// TODO migrate to the new building pattern HttpAsyncClients.custom(),
// RequestConfig, etc.
// Nowadays in Beta, wait until an stable release is delivered
public class DefaultAsyncClient extends BaseClient implements AsyncClient
{

   static class RequestProducerImpl extends BasicAsyncRequestProducer
   {
      public RequestProducerImpl(HttpHost target, HttpRequest request)
      {
         super(target, request);
      }

      protected RequestProducerImpl(HttpHost target, HttpEntityEnclosingRequest request,
            HttpAsyncContentProducer producer)
      {
         super(target, request, producer);
      }
   }

   private static HttpAsyncClient createDefaultHttpClient()
   {
      HttpAsyncClient httpClient;

      if (Registry.isRegitered(HttpAsyncClient.class)) {

         httpClient = Registry.lookup(HttpAsyncClient.class);

      } else {
         try {
            // Defaults to PoolingClientAsyncConnectionManager
            DefaultHttpAsyncClient hc = new DefaultHttpAsyncClient();
            HttpParams params = hc.getParams();
            HttpProtocolParamBean protocolBean = new HttpProtocolParamBean(params);
            VersionInfo versionInfo = VersionInfo.loadVersionInfo("glaze", DefaultAsyncClient.class.getClassLoader());
            protocolBean.setUserAgent(String.format("Glaze-AsyncHttpClient/%s", versionInfo.getRelease()));

            httpClient = hc;

         } catch (IOReactorException e) {
            throw new GlazeException(e);
         }
      }

      return httpClient;
   }

   private HttpAsyncClient httpClient;

   public DefaultAsyncClient()
   {
      this(createDefaultHttpClient());
   }

   public DefaultAsyncClient(HttpAsyncClient httpClient)
   {
      super();
      this.httpClient = httpClient;
   }

   public Client authPreemptive(String schemeName)
   {
      DefaultHttpAsyncClient httpClient = (DefaultHttpAsyncClient) getHttpClient();
      httpClient.addRequestInterceptor(new PreemptiveAuthorizer(schemeName), 0);
      return this;
   }

   @Override
   public HttpAsyncRequestProducer createAsyncProducer(HttpUriRequest request)
   {
      HttpAsyncRequestProducer producer;
      HttpHost target = URIUtils.extractHost(request.getURI());

      if (RequestUtil.isEnclosingEntity(request)) {
         producer = createRequestProducer(request, target);
      } else {
         producer = new RequestProducerImpl(target, request);
      }

      return producer;
   }

   @Override
   public <T> Future<T> execute(HttpAsyncRequestProducer producer, HttpAsyncResponseConsumer<T> consumer)
   {
      return execute(producer, consumer, null);
   }

   @Override
   public <T> Future<T> execute(HttpAsyncRequestProducer producer, HttpAsyncResponseConsumer<T> consumer,
         FutureCallback<T> futureCallback)
   {
      return execute(producer, consumer, prepareLocalContext(), futureCallback);
   }

   @Override
   public <T> Future<T> execute(HttpAsyncRequestProducer producer, HttpAsyncResponseConsumer<T> consumer,
         HttpContext context, FutureCallback<T> futureCallback)
   {
      return activateIfNeeded().execute(producer, consumer, context, futureCallback);
   }

   @Override
   public Future<Response> execute(HttpUriRequest request, ErrorHandler errorHandler)
   {
      return execute(request, null, errorHandler);
   }

   @Override
   public Future<Response> execute(HttpUriRequest request, FutureCallback<Response> futureCallback,
         ErrorHandler errorHandler)
   {
      return execute(request, prepareLocalContext(), futureCallback, errorHandler);
   }

   @Override
   public Future<Response> execute(HttpUriRequest request, HttpContext context,
         FutureCallback<Response> futureCallback, ErrorHandler errorHandler)
   {
      return activateIfNeeded().execute(createAsyncProducer(request), new ResponseConsumer(errorHandler), context, futureCallback);
   }

   public HttpAsyncClient getHttpClient()
   {
      return this.httpClient;
   }

   @Override
   public Client interceptRequest(HttpRequestInterceptor interceptor)
   {
      ((AbstractHttpAsyncClient) getHttpClient()).addRequestInterceptor(interceptor);
      return this;
   }

   @Override
   public Client interceptRequest(HttpRequestInterceptor interceptor, int position)
   {
      ((AbstractHttpAsyncClient) getHttpClient()).addRequestInterceptor(interceptor, position);
      return this;
   }

   @Override
   public Client interceptResponse(HttpResponseInterceptor interceptor)
   {
      ((AbstractHttpAsyncClient) getHttpClient()).addResponseInterceptor(interceptor);
      return this;
   }

   @Override
   public Client interceptResponse(HttpResponseInterceptor interceptor, int position)
   {
      ((AbstractHttpAsyncClient) getHttpClient()).addResponseInterceptor(interceptor, position);
      return this;
   }

   @Override
   public <T> Future<T> map(AsyncMap<T> mapRequest)
   {
      HttpContext context = mapRequest.hasContext() ? mapRequest.getContext() : prepareLocalContext();
      return activateIfNeeded().execute(createAsyncProducer(mapRequest.getRequest()), mapRequest.getConsumer(), context, mapRequest.getFutureCallback());
   }

   public void proxyAuthPref(String... authpref)
   {
      Preconditions.checkNotNull(authpref, "Please, specify a valid auth policy chain.");
      httpClient.getParams().setParameter(AuthPNames.PROXY_AUTH_PREF, Arrays.asList(authpref));
   }

   @Override
   public void registerAuthScheme(String schemeName, AuthSchemeFactory schemeFactory)
   {
      ((AbstractHttpAsyncClient) httpClient).getAuthSchemes().register(schemeName, schemeFactory);
   }

   public void registerScheme(final AsyncScheme scheme)
   {
      httpClient.getConnectionManager().getSchemeRegistry().register(scheme);
   }

   public AsyncClient reset()
   {
      shutdown();
      httpClient = createDefaultHttpClient();
      return this;
   }

   public void shutdown()
   {
      try {
         httpClient.shutdown();
      } catch (InterruptedException e) {
         //
      }
   }

   public void trustSelfSignedCertificates()
   {
      try {
         SSLLayeringStrategy sslls = new SSLLayeringStrategy(new TrustSelfSignedStrategy(), SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
         registerScheme(new AsyncScheme("https", 443, sslls));
      } catch (Exception e) {
         throw new GlazeException(e);
      }
   }

   public void unregisterScheme(final String name)
   {
      httpClient.getConnectionManager().getSchemeRegistry().unregister(name);
   }

   @Override
   protected BasicHttpContext prepareLocalContext()
   {
      BasicHttpContext ctx = super.prepareLocalContext();
      // XXX check this
      ctx.removeAttribute(ClientContext.AUTH_CACHE);
      return ctx;
   }

   private HttpAsyncClient activateIfNeeded()
   {
      IOReactorStatus status = httpClient.getStatus();
      if (IOReactorStatus.INACTIVE.equals(status)) {
         httpClient.start();
      }
      return httpClient;
   }

   private HttpAsyncRequestProducer createRequestProducer(HttpUriRequest request, HttpHost target)
   {
      HttpAsyncRequestProducer producer;
      HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest) request;
      HttpEntity entity = entityRequest.getEntity();

      if (HttpAsyncContentProducer.class.isAssignableFrom(entity.getClass())) {
         producer = new RequestProducerImpl(target, entityRequest, (HttpAsyncContentProducer) entity);
      } else if (MultipartEntity.class.isAssignableFrom(entity.getClass())) {
         // TODO investigate zero copy multipart (not zero copy raw file
         // transfer...)
         try {
            producer = new RequestProducerImpl(target, entityRequest, new EntityAsyncContentProducer(new BufferedMultipartEntity((MultipartEntity) entity)));
         } catch (IOException e) {
            throw new GlazeException(e);
         }
      } else {
         producer = new RequestProducerImpl(target, entityRequest, new EntityAsyncContentProducer(entity));
      }

      return producer;
   }

}
