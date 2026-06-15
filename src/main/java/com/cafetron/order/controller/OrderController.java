package com.cafetron.order.controller;

import com.cafetron.order.dto.MyOrderSummaryResponse;
import com.cafetron.order.dto.OrderDetailResponse;
import com.cafetron.order.dto.PlaceOrderRequest;
import com.cafetron.order.dto.PlaceOrderResponse;
import com.cafetron.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    // When Jwt is configured, this will change
    public PlaceOrderResponse placeOrder(@RequestParam Long userId, @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(userId,request);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<MyOrderSummaryResponse> getMyOrderSummary(@RequestParam Long userId){
        return orderService.getMyOrders(userId);
    }

    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public OrderDetailResponse getOrderDetail(@RequestParam Long userId, @PathVariable Long orderId){
        return orderService.getOrderDetail(userId, orderId);
    }

    @PostMapping("/{orderId}/timeout")
    @ResponseStatus(HttpStatus.OK)
    public OrderDetailResponse processTimeout(@RequestParam Long userId, @PathVariable Long orderId) {
        return orderService.processTimeout(userId, orderId);
    }

}
