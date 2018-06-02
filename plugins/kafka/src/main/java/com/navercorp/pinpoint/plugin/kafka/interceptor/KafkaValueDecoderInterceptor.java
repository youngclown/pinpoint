/**
 * Copyright 2014 NAVER Corp.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.navercorp.pinpoint.plugin.kafka.interceptor;

import java.security.ProtectionDomain;
import java.util.Map;

import com.navercorp.pinpoint.bootstrap.context.*;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentMethod;
import com.navercorp.pinpoint.bootstrap.instrument.Instrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.transformer.TransformCallback;
import com.navercorp.pinpoint.bootstrap.interceptor.SpanSimpleAroundInterceptor;
import com.navercorp.pinpoint.bootstrap.util.NumberUtils;
import com.navercorp.pinpoint.plugin.kafka.KafkaHeaderGetter;
import com.navercorp.pinpoint.plugin.kafka.KafkaHeaderSetter;
import com.navercorp.pinpoint.plugin.kafka.KafkaPluginConstants;

/**
 * @author Jiaqi Feng
 */

public class KafkaValueDecoderInterceptor extends SpanSimpleAroundInterceptor {

    public KafkaValueDecoderInterceptor(TraceContext traceContext, MethodDescriptor descriptor) {
        super(traceContext, descriptor, KafkaValueDecoderInterceptor.class);
    }

    @Override
    protected Trace createTrace(Object target, Object[] args) {
        if (args ==null || !(args[0] instanceof byte[])) {
            logger.error("KafkaValueDecoderInterceptor.createTrace(): args or args[0] is not what expected, return no trace");
            return null;
        }

        String value=new String((byte[])args[0]);
        System.out.println("KafkaValueDecoderInterceptor.createTrace(): got value="+value);
        int pos=value.indexOf("\n");
        if (pos == -1) {
            System.out.println("KafkaValueDecoderInterceptor.createTrace(): no header, return no trace");
            return null;
        }

        String[] headers = value.substring(0,pos).split("\\^");
        logger.error("KafkaValueDecoderInterceptor.createTrace(): header contains "+headers.length+" field");
        for (String s : headers)
            logger.error("KafkaValueDecoderInterceptor.createTrace(): get id="+s);
        if (headers.length < 8) {
            System.out.println("KafkaValueDecoderInterceptor.createTrace(): header fileds not enough, return no trace");
            return null;
        }

        String transactionId = headers[0]+"^"+headers[1]+"^"+headers[2];
        long parentSpanID = NumberUtils.parseLong(headers[3], SpanId.NULL);
        long spanID = NumberUtils.parseLong(headers[4], SpanId.NULL);
        short flags = NumberUtils.parseShort(headers[7], (short) 0);

        TraceId traceId = traceContext.createTraceId(transactionId, parentSpanID, spanID, flags);

        logger.error("KafkaValueDecoderInterceptor.createTrace(): parentSpan="+parentSpanID+", spanId="+spanID+",traceId="+traceId);
        return traceContext.continueTraceObject(traceId);
    }

    @Override
    protected void doInBeforeTrace(SpanRecorder recorder, Object target, Object[] args) {
        // You have to record a service type within Server range.
        recorder.recordServiceType(KafkaPluginConstants.KAFKA_SERVICE_TYPE);
        String exchange="unknown";// TODO should be kafka hostname or ip
        if (exchange == null || exchange.equals("")) exchange = "unknown";
        // Record client address, server address.
        recorder.recordEndPoint(exchange);

        recorder.recordParentApplication(exchange, KafkaPluginConstants.KAFKA_SERVICE_TYPE.getCode());
        recorder.recordAcceptorHost(exchange);
        logger.error("fengjiaqi KafkaValueDecoderInterceptor: doInBefore endPoint="+exchange);
    }

    @Override
    protected void doInAfterTrace(SpanRecorder recorder, Object target, Object[] args, Object result, Throwable throwable) {
        String header=null;

        recorder.recordApi(methodDescriptor);
        recorder.recordRemoteAddress("unknownip:port");

        if (throwable != null) {
            recorder.recordException(throwable);
        }
    }
}
