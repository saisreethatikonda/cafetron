package com.cafetron.order.service;

import com.cafetron.order.dto.*;

import java.util.List;

public interface OrderService {
    PlaceOrderResponse placeOrder(Long userId, PlaceOrderRequest request);
    List<MyOrderSummaryResponse> getMyOrders(Long userId);
    OrderDetailResponse getOrderDetail(Long userId, Long orderId);
    OrderDetailResponse processTimeout(Long userId, Long orderId);
}