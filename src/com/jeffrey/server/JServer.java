package com.jeffrey.server;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by jeffrey on 3/7/15.
 */
public class JServer{
    HttpServer server;

    public JServer(int port) throws IOException {
        this(port, 0);
    }

    public JServer(int port, int i) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 5);
        //server.start();
    }


    public void start(){
        server.start();
    }

    public void stop(){
        server.stop(0);
    }

    public HttpContext register(String s, JHandler h){
        return server.createContext(s, new HandlerWrapper(h));
    }


    public class HandlerWrapper implements HttpHandler {
        JHandler h;

        public HandlerWrapper(JHandler h){
            this.h = h;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            //System.out.println("Forwarding...");
            Response r = h.handle(new Request(httpExchange));
            if(r == null)
                r = new Response(500);
            httpExchange.sendResponseHeaders(r.getStatus(), r.getSize());
            httpExchange.getResponseBody().write(r.getBody());
            httpExchange.getResponseBody().close();
        }
    }
}