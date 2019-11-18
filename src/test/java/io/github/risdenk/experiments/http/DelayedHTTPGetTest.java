package io.github.risdenk.experiments.http;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class DelayedHTTPGetTest {
  private static Server server;
  private static URI serverUri;

  @BeforeAll
  static void startJetty() throws Exception {
    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(0); // auto-bind to available port
    server.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler();
    ServletHolder defaultServ = new ServletHolder("default", DelayedHttpGetServlet.class);
    context.addServlet(defaultServ, "/");
    server.setHandler(context);

    // Start Server
    server.start();

    // Determine Base URI for Server
    String host = connector.getHost();
    if (host == null)
    {
      host = "localhost";
    }
    int port = connector.getLocalPort();
    serverUri = new URI(String.format("http://%s:%d/", host, port));
  }

  @AfterAll
  static void stopJetty() throws Exception {
    server.stop();
  }

  @Test
  void testGet() throws Exception {
    HttpURLConnection http = (HttpURLConnection) serverUri.resolve("/").toURL().openConnection();
    http.setReadTimeout(1000);
    http.connect();
    Assertions.assertThrows(SocketTimeoutException.class, http::getResponseCode);
  }

  @Test
  void testHttpClientGet() throws Exception {
    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      HttpGet httpGet = new HttpGet(serverUri);
      RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(1000).build();
      httpGet.setConfig(requestConfig);
      Assertions.assertThrows(SocketTimeoutException.class, () -> httpclient.execute(httpGet));
    }
  }

  @Test
  void testOkHttpClientGet() {
    OkHttpClient client = new OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build();
    Request request = new Request.Builder().url(serverUri.toString()).build();
    Assertions.assertThrows(SocketTimeoutException.class, () -> client.newCall(request).execute());
  }

  @Test
  void testJettyHttpClientGet() throws Exception {
    HttpClient httpClient = new HttpClient();
    try {
      httpClient.start();
      Assertions.assertThrows(TimeoutException.class, () ->
          httpClient.newRequest(serverUri).idleTimeout(1, TimeUnit.SECONDS).send());
    } finally {
      httpClient.stop();
    }
  }

  public static class DelayedHttpGetServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
      try {
        TimeUnit.SECONDS.sleep(5);
        resp.setContentType(MimeTypes.Type.TEXT_HTML.asString());
        resp.setStatus(HttpServletResponse.SC_OK);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
