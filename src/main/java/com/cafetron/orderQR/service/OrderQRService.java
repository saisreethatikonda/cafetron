package com.cafetron.orderQR.service;

import com.cafetron.order.Order;
import org.springframework.web.multipart.MultipartFile;

public interface OrderQRService {

    String generateAndStoreQR(Order order);

    String decodeQRFromImage(MultipartFile file);
}
