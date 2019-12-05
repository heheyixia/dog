package com.test;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;

import java.util.HashSet;
import java.util.Set;

public class MainVerticle extends AbstractVerticle {


  static JsonObject server_data = new JsonObject();


  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    vertx.setPeriodic(5000, doing -> {
      System.out.println(Runtime.getRuntime().totalMemory() / 1024 / 1024 + "--totalMemory");
      System.out.println(Runtime.getRuntime().freeMemory() / 1024 / 1024 + "--freeMemory");
      System.out.println(Runtime.getRuntime().maxMemory() / 1024 / 1024 + "--maxMemory");
    });


//    Router router = Router.router(vertx);
    vertx.createHttpServer().requestHandler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    }).websocketHandler(this::dealWebSocket).listen(8888, http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }

  private void mergeData(String msg, ServerWebSocket socket) {
    server_data.put(socket.path(), msg);
  }

  private void startPeriodic(ServerWebSocket socket) {
    vertx.setPeriodic(25, deal -> {
      if (!socket.isClosed()) {
        String d = server_data.getString(socket.path());
        if (d != null && d != "") {
          socket.writeTextMessage(d);
        }
      } else {
        vertx.cancelTimer(deal);
        System.out.println("cancel  timer");
      }
    });
  }

  private void syncDataOnConnect(ServerWebSocket socket) {
    if (!socket.isClosed()) {
      String d = server_data.getString(socket.path());
      if (d != null && d != "") {
        socket.writeTextMessage(d);
      }
    }
  }


  void dealWebSocket(ServerWebSocket con) {
    System.out.println(con.path());
    server_data.put(con.path(), "");

    con.accept();
    startPeriodic(con);

    con.textMessageHandler(msg -> {
      mergeData(msg, con);
    });
  }


  public Set<String> getAllowedHeaders() {
    Set<String> allowHeaders = new HashSet<>();
    allowHeaders.add("X-Requested-With");
    allowHeaders.add("Access-Control-Allow-Origin");
    allowHeaders.add("Origin");
    allowHeaders.add("Content-Type");
    allowHeaders.add("Accept");
    allowHeaders.add(HttpHeaders.AUTHORIZATION.toString());
    return allowHeaders;
  }

  public static void main(String[] args) throws Exception {
    Launcher.executeCommand("run", "com.test.starter.MainVerticle");
  }
}
