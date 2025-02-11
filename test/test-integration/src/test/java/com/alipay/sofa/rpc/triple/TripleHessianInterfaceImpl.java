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

import com.alipay.common.tracer.core.holder.SofaTraceContextHolder;
import com.alipay.common.tracer.core.span.SofaTracerSpan;
import com.alipay.common.tracer.core.utils.TracerUtils;

/**
 * @author zhaowang
 * @version : TripleHessianInterfaceImpl.java, v 0.1 2020年06月11日 11:29 上午 zhaowang Exp $
 */
public class TripleHessianInterfaceImpl implements TripleHessianInterface {

    private String flag;

    public TripleHessianInterfaceImpl() {
        this.flag = "";
    }

    @Override
    public void call() {
        this.flag = "call";
    }

    @Override
    public String call1() {
        this.flag = "call1";
        return flag;
    }

    @Override
    public Response call2(Request request) {
        if (request != null) {
            this.flag = request.getFlag();
        } else {
            flag = null;
        }
        Response read = Response.read(request);
        return read;
    }

    public String getFlag() {
        return flag;
    }

    @Override
    public boolean testPressureMark(String name) {
        SofaTracerSpan currentSpan = SofaTraceContextHolder.getSofaTraceContext().getCurrentSpan();
        return TracerUtils.isLoadTest(currentSpan);
    }
}