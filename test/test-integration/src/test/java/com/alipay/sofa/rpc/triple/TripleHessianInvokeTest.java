/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.rpc.triple;

import com.alipay.common.tracer.core.SofaTracer;
import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.common.tracer.core.utils.TracerUtils;
import com.alipay.sofa.rpc.common.RpcConstants;
import com.alipay.sofa.rpc.config.ApplicationConfig;
import com.alipay.sofa.rpc.config.ConsumerConfig;
import com.alipay.sofa.rpc.config.ProviderConfig;
import com.alipay.sofa.rpc.config.ServerConfig;
import com.alipay.sofa.rpc.constant.TripleConstant;
import com.alipay.sofa.rpc.context.RpcRunningState;
import com.alipay.sofa.rpc.log.Logger;
import com.alipay.sofa.rpc.log.LoggerFactory;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * @author zhaowang
 * @version : TripleHessianInvokeTest.java, v 0.1 2020年06月11日 11:16 上午 zhaowang Exp $
 */
public class TripleHessianInvokeTest {

    private static final Logger    LOGGER = LoggerFactory.getLogger(TripleHessianInvokeTest.class);
    public static final SofaTracer tracer = new SofaTracer.Builder("TEST").build();

    @Before
    public void before() {
        SofaTracerSpan span = (SofaTracerSpan) tracer.buildSpan("test").start();
        SofaTraceContextHolder.getSofaTraceContext().push(span);
    }

    @Test
    public void testInvoke() throws InterruptedException {
        RpcRunningState.setDebugMode(true);

        ApplicationConfig clientApp = new ApplicationConfig().setAppName("triple-client");

        ApplicationConfig serverApp = new ApplicationConfig().setAppName("triple-server");

        int port = 50062;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        TripleHessianInterfaceImpl ref = new TripleHessianInterfaceImpl();
        ProviderConfig<TripleHessianInterface> providerConfig = new ProviderConfig<TripleHessianInterface>()
            .setApplication(serverApp)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(TripleHessianInterface.class.getName())
            .setRef(ref)
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.export();

        ConsumerConfig<TripleHessianInterface> consumerConfig = new ConsumerConfig<TripleHessianInterface>();
        consumerConfig.setInterfaceId(TripleHessianInterface.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("localhost:" + port)
            .setRegister(false)
            .setApplication(clientApp);

        TripleHessianInterface helloService = consumerConfig.refer();

        LOGGER.info("Grpc stub bean successful: {}", helloService.getClass().getName());
        helloService.call();
        Assert.assertEquals("call", ref.getFlag());

        // test Pressure Mark
        boolean isLoadTest = helloService.testPressureMark("name");
        Assert.assertFalse(isLoadTest);

        SofaTracerSpan currentSpan = SofaTraceContextHolder.getSofaTraceContext().getCurrentSpan();
        Map<String, String> bizBaggage = currentSpan.getSofaTracerSpanContext().getBizBaggage();
        bizBaggage.put("mark", "T");
        Assert.assertTrue(TracerUtils.isLoadTest(currentSpan));
        isLoadTest = helloService.testPressureMark("name");
        Assert.assertTrue(isLoadTest);

        String s = helloService.call1();
        Assert.assertEquals("call1", ref.getFlag());
        Assert.assertEquals("call1", s);

        Request request = new Request();
        int age = RandomUtils.nextInt();
        request.setAge(age);
        String call2 = "call2";
        request.setFlag(call2);
        Response response = helloService.call2(request);
        Assert.assertEquals(age, response.getAge());
        Assert.assertEquals(call2, response.getFlag());

        Response response1 = helloService.call2(null);
        Assert.assertNull(response1);

        providerConfig.unExport();
        serverConfig.destroy();
    }

    @Test
    public void testInvokeWithUniqueId() throws InterruptedException {
        String uniqueId = "uniqueId1";
        RpcRunningState.setDebugMode(true);

        ApplicationConfig clientApp = new ApplicationConfig().setAppName("triple-client");

        ApplicationConfig serverApp = new ApplicationConfig().setAppName("triple-server");

        int port = 50063;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        TripleHessianInterfaceImpl ref = new TripleHessianInterfaceImpl();
        ProviderConfig<TripleHessianInterface> providerConfig = new ProviderConfig<TripleHessianInterface>()
            .setApplication(serverApp)
            .setUniqueId(uniqueId)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(TripleHessianInterface.class.getName())
            .setRef(ref)
            .setRepeatedExportLimit(-1)
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.setParameter(TripleConstant.TRIPLE_EXPOSE_OLD, "true");

        providerConfig.export();

        ConsumerConfig<TripleHessianInterface> consumerConfig = new ConsumerConfig<TripleHessianInterface>();
        consumerConfig.setInterfaceId(TripleHessianInterface.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("localhost:" + port)
            .setUniqueId(uniqueId)
            .setRegister(false)
            .setApplication(clientApp);

        TripleHessianInterface helloService = consumerConfig.refer();

        LOGGER.info("Grpc stub bean successful: {}", helloService.getClass().getName());
        helloService.call();
        Assert.assertEquals("call", ref.getFlag());

        String s = helloService.call1();
        Assert.assertEquals("call1", ref.getFlag());
        Assert.assertEquals("call1", s);

        Request request = new Request();
        int age = RandomUtils.nextInt();
        request.setAge(age);
        String call2 = "call2";
        request.setFlag(call2);
        Response response = helloService.call2(request);
        Assert.assertEquals(age, response.getAge());
        Assert.assertEquals(call2, response.getFlag());

        Response response1 = helloService.call2(null);
        Assert.assertNull(response1);

        // 测试没有设置 uniqueId 的情况，也能访问
        consumerConfig = new ConsumerConfig<TripleHessianInterface>();
        consumerConfig.setInterfaceId(TripleHessianInterface.class.getName())
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setDirectUrl("localhost:" + port)
            .setRegister(false)
            .setApplication(clientApp);

        helloService = consumerConfig.refer();

        LOGGER.info("Grpc stub bean successful: {}", helloService.getClass().getName());
        helloService.call();
        Assert.assertEquals("call", ref.getFlag());

        s = helloService.call1();
        Assert.assertEquals("call1", ref.getFlag());
        Assert.assertEquals("call1", s);

        request = new Request();
        age = RandomUtils.nextInt();
        request.setAge(age);
        call2 = "call2";
        request.setFlag(call2);
        response = helloService.call2(request);
        Assert.assertEquals(age, response.getAge());
        Assert.assertEquals(call2, response.getFlag());

        response1 = helloService.call2(null);
        Assert.assertNull(response1);

        providerConfig.unExport();
        serverConfig.destroy();
    }

    @Test
    public void testExposeTwice() {
        String uniqueId = "uniqueId";
        RpcRunningState.setDebugMode(true);

        ApplicationConfig clientApp = new ApplicationConfig().setAppName("triple-client");

        ApplicationConfig serverApp = new ApplicationConfig().setAppName("triple-server");

        int port = 50062;

        ServerConfig serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        TripleHessianInterfaceImpl ref = new TripleHessianInterfaceImpl();
        ProviderConfig<TripleHessianInterface> providerConfig = new ProviderConfig<TripleHessianInterface>()
            .setApplication(serverApp)
            .setUniqueId(uniqueId)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(TripleHessianInterface.class.getName())
            .setRef(ref)
            .setRepeatedExportLimit(-1)
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig.setParameter(TripleConstant.TRIPLE_EXPOSE_OLD, "true");

        providerConfig.export();

        uniqueId = "uniqueId2";
        RpcRunningState.setDebugMode(true);

        clientApp = new ApplicationConfig().setAppName("triple-client");

        serverApp = new ApplicationConfig().setAppName("triple-server");

        port = 50062;

        serverConfig = new ServerConfig()
            .setProtocol(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setPort(port);

        ref = new TripleHessianInterfaceImpl();
        ProviderConfig<TripleHessianInterface> providerConfig2 = new ProviderConfig<TripleHessianInterface>()
            .setApplication(serverApp)
            .setUniqueId(uniqueId)
            .setBootstrap(RpcConstants.PROTOCOL_TYPE_TRIPLE)
            .setInterfaceId(TripleHessianInterface.class.getName())
            .setRef(ref)
            .setServer(serverConfig)
            .setRegister(false);

        providerConfig2.setParameter(TripleConstant.TRIPLE_EXPOSE_OLD, "true");

        try {
            providerConfig2.export();
            Assert.fail();
        } catch (Exception e) {

        } finally {
            providerConfig2.unExport();
            providerConfig.unExport();
            serverConfig.destroy();

        }
    }
}
