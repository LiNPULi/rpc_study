package com.ydlclass.channelhandler.handler;

import com.ydlclass.YrpcBootstrap;
import com.ydlclass.enumeration.RespCode;
import com.ydlclass.exceptions.ResponseException;
import com.ydlclass.loadbalancer.LoadBalancer;
import com.ydlclass.protection.CircuitBreaker;
import com.ydlclass.transport.message.YrpcRequest;
import com.ydlclass.transport.message.YrpcResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 这是一个用来测试的类
 *
 * @author it楠老师
 * @createTime 2023-07-02
 */
@Slf4j
public class MySimpleChannelInboundHandler extends SimpleChannelInboundHandler<YrpcResponse> {
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, YrpcResponse yrpcResponse) throws Exception {
    
        // 从全局的挂起的请求中寻找与之匹配的待处理的completableFuture
        CompletableFuture<Object> completableFuture = YrpcBootstrap.PENDING_REQUEST.get(yrpcResponse.getRequestId());
        
        SocketAddress socketAddress = channelHandlerContext.channel().remoteAddress();
        Map<SocketAddress, CircuitBreaker> everyIpCircuitBreaker = YrpcBootstrap.getInstance()
            .getConfiguration().getEveryIpCircuitBreaker();
        CircuitBreaker circuitBreaker = everyIpCircuitBreaker.get(socketAddress);
    
        byte code = yrpcResponse.getCode();
        if(code == RespCode.FAIL.getCode()){
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为[{}]的请求，返回错误的结果，响应码[{}].",
                yrpcResponse.getRequestId(),yrpcResponse.getCode());
            throw new ResponseException(code,RespCode.FAIL.getDesc());
            
        } else if (code == RespCode.RATE_LIMIT.getCode()){
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为[{}]的请求，被限流，响应码[{}].",
                yrpcResponse.getRequestId(),yrpcResponse.getCode());
            throw new ResponseException(code,RespCode.RATE_LIMIT.getDesc());
            
        } else if (code == RespCode.RESOURCE_NOT_FOUND.getCode() ){
            circuitBreaker.recordErrorRequest();
            completableFuture.complete(null);
            log.error("当前id为[{}]的请求，未找到目标资源，响应码[{}].",
                yrpcResponse.getRequestId(),yrpcResponse.getCode());
            throw new ResponseException(code,RespCode.RESOURCE_NOT_FOUND.getDesc());
            
        } else if (code == RespCode.SUCCESS.getCode() ){
            // 服务提供方，给予的结果
            Object returnValue = yrpcResponse.getBody();
            completableFuture.complete(returnValue);
            if (log.isDebugEnabled()) {
                log.debug("以寻找到编号为【{}】的completableFuture，处理响应结果。", yrpcResponse.getRequestId());
            }
        } else if(code == RespCode.SUCCESS_HEART_BEAT.getCode()){
            completableFuture.complete(null);
            if (log.isDebugEnabled()) {
                log.debug("以寻找到编号为【{}】的completableFuture,处理心跳检测，处理响应结果。", yrpcResponse.getRequestId());
            }
        } else if(code == RespCode.BECOLSING.getCode()){
            completableFuture.complete(null);
            if (log.isDebugEnabled()) {
                log.debug("当前id为[{}]的请求，访问被拒绝，目标服务器正处于关闭中，响应码[{}].",
                    yrpcResponse.getRequestId(),yrpcResponse.getCode());
            }
            
            // 修正负载均衡器
            // 从健康列表中移除
            YrpcBootstrap.CHANNEL_CACHE.remove(socketAddress);
            // reLoadBalance
            LoadBalancer loadBalancer = YrpcBootstrap.getInstance()
                .getConfiguration().getLoadBalancer();
            // 重新进行负载均衡
            YrpcRequest yrpcRequest = YrpcBootstrap.REQUEST_THREAD_LOCAL.get();
            loadBalancer.reLoadBalance(yrpcRequest.getRequestPayload().getInterfaceName()
                ,YrpcBootstrap.CHANNEL_CACHE.keySet().stream().toList());
    
            throw new ResponseException(code,RespCode.BECOLSING.getDesc());
        }
    }
}
