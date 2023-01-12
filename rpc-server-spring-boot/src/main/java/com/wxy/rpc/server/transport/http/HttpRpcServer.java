package com.wxy.rpc.server.transport.http;

import com.wxy.rpc.core.exception.RpcException;
import com.wxy.rpc.server.transport.RpcServer;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.Server;
import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardEngine;
import org.apache.catalina.core.StandardHost;
import org.apache.catalina.startup.Tomcat;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 基于 HTTP 通信协议实现的 Rpc Server 类
 *
 * @author Wuxy
 * @version 1.0
 * @ClassName HttpRpcServer
 * @Date 2023/1/7 11:11
 */
public class HttpRpcServer implements RpcServer {

    @Override
    public void start(Integer port) {
        try {
            Tomcat tomcat = new Tomcat();

            Server server = tomcat.getServer();
            Service service = server.findService("Tomcat");

            Connector connector = new Connector();
            connector.setPort(port);

            String hostname = InetAddress.getLocalHost().getHostAddress();

            StandardEngine engine = new StandardEngine();
            engine.setDefaultHost(hostname);

            StandardHost host = new StandardHost();
            host.setName(hostname);

            String contextPath = "";
            Context context = new StandardContext();
            context.setPath(contextPath);
            context.addLifecycleListener(new Tomcat.FixContextListener());

            host.addChild(context);
            engine.addChild(host);

            service.setContainer(engine);
            service.addConnector(connector);

            tomcat.addServlet(contextPath, "dispatcher", new DispatcherServlet());
            context.addServletMappingDecoded("/*", "dispatcher");

            tomcat.start();
            tomcat.getServer().await();
        } catch (LifecycleException | UnknownHostException e) {
            throw new RpcException("Tomcat server failed to start.", e);
        }
    }

}
