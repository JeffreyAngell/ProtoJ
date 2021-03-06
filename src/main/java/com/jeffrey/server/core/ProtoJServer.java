package com.jeffrey.server.core;

import com.sun.net.httpserver.*;

import javax.net.ssl.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;

public class ProtoJServer {
    HttpServer server;

    public ProtoJServer(int port) throws IOException {
        this(port, 0);
    }

    public ProtoJServer(int port, int i) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 5);
    }

    public ProtoJServer(int port, String s, String kslocation, String kspassword) throws IOException {
        if (s.equals("https") || s.contains("s")) {
            server = HttpsServer.create(new InetSocketAddress(port), 0);
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");

                // initialise the keystore
                char[] password = kspassword.toCharArray();
                KeyStore ks = KeyStore.getInstance("JKS");
                FileInputStream fis = new FileInputStream(kslocation);
                ks.load(fis, password);

                // setup the key manager factory
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                kmf.init(ks, password);

                // setup the trust manager factory
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(ks);

                // setup the HTTPS context and parameters
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                ((HttpsServer) server).setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                    public void configure(HttpsParameters params) {
                        try {
                            // initialise the SSL context
                            SSLContext c = SSLContext.getDefault();
                            SSLEngine engine = c.createSSLEngine();
                            params.setNeedClientAuth(false);
                            params.setCipherSuites(engine.getEnabledCipherSuites());
                            params.setProtocols(engine.getEnabledProtocols());

                            // get the default parameters
                            SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
                            params.setSSLParameters(defaultSSLParameters);
                        } catch (NoSuchAlgorithmException e1) {
                            e1.printStackTrace();
                            System.exit(0);
                        }
                    }
                });
            } catch (NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException | CertificateException | KeyManagementException e) {
                e.printStackTrace();
                System.exit(0);
            }
        } else{
            server = HttpServer.create(new InetSocketAddress(port), 0);
        }
    }


    public void start(){
        server.start();
    }

    public void stop(){
        server.stop(0);
    }

    public HttpContext register(String s, ProtoJHandler h){
        return server.createContext(s, new HandlerWrapper(h));
    }


    public class HandlerWrapper implements HttpHandler {
        ProtoJHandler h;

        public HandlerWrapper(ProtoJHandler h){
            this.h = h;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            Response r = h.handle(new Request(httpExchange));
            if(r == null)
                r = new Response(500);
            Headers h = r.getHeaders();
            if(h != null){
                Headers responseHeaders = httpExchange.getResponseHeaders();
                for(String key: h.keySet()){
                    responseHeaders.add(key, h.getFirst(key));
                }
            }
            httpExchange.sendResponseHeaders(r.getStatus(), r.getSize());
            if(!r.isStream()) {
                httpExchange.getResponseBody().write(r.getBody());
            }
            else{
                byte[] b = new byte[2048];
                InputStream is = r.getStream();
                while(is.available() > 0) {
                    int read = is.read(b);
                    httpExchange.getResponseBody().write(b, 0, read);
                }
            }
            httpExchange.getResponseBody().close();
        }
    }

    public int getPort(){
        return server.getAddress().getPort();
    }
}
