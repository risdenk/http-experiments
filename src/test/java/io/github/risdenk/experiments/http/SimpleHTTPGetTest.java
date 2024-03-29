package io.github.risdenk.experiments.http;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http.HttpStatus;
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
import java.net.URI;

class SimpleHTTPGetTest {
  private static Server server;
  private static URI serverUri;

  @BeforeAll
  static void startJetty() throws Exception {
    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(0); // auto-bind to available port
    server.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler();
    ServletHolder defaultServ = new ServletHolder("default", HttpGetServlet.class);
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
    http.connect();
    Assertions.assertEquals(HttpStatus.OK_200, http.getResponseCode());
  }

  @Test
  void testHttpClientGet() throws Exception {
    try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
      HttpGet httpGet = new HttpGet(serverUri);
      try (CloseableHttpResponse response = httpclient.execute(httpGet)) {
        Assertions.assertEquals(HttpStatus.OK_200, response.getStatusLine().getStatusCode());
      }
    }
  }

  @Test
  void testOkHttpClientGet() throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(serverUri.toString()).build();
    try (Response response = client.newCall(request).execute()) {
      Assertions.assertEquals(HttpStatus.OK_200, response.code());
    }
  }

  @Test
  void testJettyHttpClientGet() throws Exception {
    HttpClient httpClient = new HttpClient();
    try {
      httpClient.start();
      ContentResponse response = httpClient.GET(serverUri);
      Assertions.assertEquals(HttpStatus.OK_200, response.getStatus());
    } finally {
      httpClient.stop();
    }
  }

  public static class HttpGetServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
      resp.setContentType(MimeTypes.Type.TEXT_HTML.asString());
      resp.setStatus(HttpServletResponse.SC_OK);
    }
  }
}
