package io.github.risdenk.experiments.http;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.HttpURLConnection;
import java.net.URI;

class HttpExperimentsTest {
  private static Server server;
  private static URI serverUri;

  @BeforeAll
  static void startJetty() throws Exception {
    server = new Server();
    ServerConnector connector = new ServerConnector(server);
    connector.setPort(0); // auto-bind to available port
    server.addConnector(connector);

    ServletContextHandler context = new ServletContextHandler();
    ServletHolder defaultServ = new ServletHolder("default", DefaultServlet.class);
    defaultServ.setInitParameter("resourceBase", System.getProperty("user.dir"));
    defaultServ.setInitParameter("dirAllowed", "true");
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
}
