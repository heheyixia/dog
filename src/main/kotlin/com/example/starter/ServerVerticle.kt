package com.example.starter

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.impl.WebSocketImpl
import io.vertx.core.http.impl.WebSocketRequestHandler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.KeyStoreOptions
import io.vertx.ext.auth.KeyStoreOptionsConverter
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
import io.vertx.ext.auth.jwt.JWTKeyStoreOptionsConverter
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.Session
import io.vertx.ext.web.handler.*
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.sstore.LocalSessionStore
import io.vertx.kotlin.core.net.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.ext.auth.jwt.jwtKeyStoreOptionsOf
import java.security.KeyStore
import java.util.*
import java.util.HashSet



class ServerVerticle : CoroutineVerticle() {

  private val HTTP_PORT_DEFAULT: Int = 80;

  override fun start(startFuture: Future<Void>?) {
//    super.start(startFuture)
    serverStart(startFuture)
  }

  fun serverStart(future: Future<Void>?) {

    var router = Router.router(vertx)
    router.route().handler(CookieHandler.create())
    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)))
    val allowHeaders = HashSet<String>()
    allowHeaders.add("x-requested-with")
    allowHeaders.add("Access-Control-Allow-Origin")
    allowHeaders.add("origin")
    allowHeaders.add("Content-Type")
    allowHeaders.add("accept")

    router.route().handler(CorsHandler
      .create("*")
//      .allowCredentials(true)
      .allowedMethods(setOf(HttpMethod.OPTIONS, HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE,HttpMethod.HEAD))
      .allowedHeaders(allowHeaders)
      .maxAgeSeconds(3600)
    )
//    Access-Control-Allow-Origin
    router.route("/").handler({ req ->
      req.response().putHeader("content-type", "application/json").end(JsonObject().put("res", "000").toBuffer())
    })
    router.route("/file/*").handler(StaticHandler.create("C:/Users/Administrator/Desktop/VOID II_files/sender").setDirectoryListing(true))
//    val h = SockJSHandler.create(vertx)
//    h.socketHandler { con ->
//      con.handler { buf ->
//        println("server RECEIVE msg : ${buf.toString()}")
//      }
////      con.han
//      con.endHandler { a ->
//        println("do endHandler")
//      }
//
//    }
//    router.route().handler(WebSocketHandler)
    val server = vertx.createHttpServer()
    server.websocketHandler { socket ->
      println(socket.path())
      socket.accept()
      socket.textMessageHandler{m ->
        println("server receive : ${m}")
      }
      socket.closeHandler{c ->
        println("server catch close action")
      }
      socket.exceptionHandler{e ->
        println(e.message)
      }
    }
    server.requestHandler(router).listen(
      context.config().getInteger("HTTP_PORT", HTTP_PORT_DEFAULT),
      { res ->
        if (res.succeeded()) {
          future?.complete()
        } else {
          future?.fail(res.cause())
        }
      })

    vertx.createNetServer().connectHandler{s ->
      s.closeHandler{
        println(" socket close > server")
      }
      s.handler{
        b ->
        println(b.toString())
      }
      var state = 200;
      var jsonStr = JsonObject().put("res","123123")
      var rspHeaders = """HTTP/1.1 $state OK
Content-Type: application/json
Content-Length: ${jsonStr.toString().toByteArray().size}
Access-Control-Allow-Origin: *
"""
      vertx.setTimer(3000,{
        s.write(rspHeaders+"\n"+jsonStr)
        s.end()
      })
    }.listen(8081,{r ->
      if(r.succeeded()){
        println("netSocket succ")
      }else{
        println(333333)
      }
    })

  }


  //  .setKeyStore(new KeyStoreOptions()
//  .setType("jceks")
//  .setPath("keystore.jceks")
//  .setPassword("secret"));
  fun jwtConfig() {
    JWTAuthHandler.create(JWTAuth.create(
      vertx, JWTAuthOptions().setKeyStore(KeyStoreOptions().setPassword("12341234").setPath("")))
    )

  }

  override fun stop(stopFuture: Future<Void>?) {
    super.stop(stopFuture)
  }

}

fun main() {
  Vertx.vertx().deployVerticle("com.example.starter.ServerVerticle" ,{
    res ->
   if(res.succeeded()){
     println("1111")
   }else{
     println("222222")
   }

  })

}
