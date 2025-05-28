package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    @Scheduled(cron = "0 * * * * ?")//每分钟执行
    public void processTimeoutOrder() {
        log.info("定时处理超时订单：{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> outTimeOrders = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        if(outTimeOrders != null && outTimeOrders.size() > 0) {
            for (Orders order : outTimeOrders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("超时未支付取消订单");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    public void processDeliveryOrder() {
        log.info("定时处理派送中订单：{}", LocalDateTime.now());
        LocalDateTime time = LocalDateTime.now().plusHours(-3);
        List<Orders> outTimeOrders = orderMapper.getByStatusAndOrderTimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if(outTimeOrders != null && outTimeOrders.size() > 0) {
            for (Orders order : outTimeOrders) {
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
