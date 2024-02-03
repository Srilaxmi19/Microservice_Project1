package com.olive.orderservice.service;

import com.olive.orderservice.dto.InventoryResponse;
import com.olive.orderservice.dto.OrderLineItemsDto;
import com.olive.orderservice.dto.OrderRequest;
import com.olive.orderservice.model.Order;
import com.olive.orderservice.model.OrderLineItems;
import com.olive.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private WebClient webClient;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                .map(OrderLineItems::getSkuCode)
                .toList();


        InventoryResponse[] inventoryResponseArray = webClient.get()
                .uri("http://localhost:8082/api/v1/inventory",uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        // Call inventory service, and place order if the stock is available i.e, if result is true
        boolean allproductsArray= Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);
        if(allproductsArray){
            orderRepository.save(order);
        }
        else{
            throw new IllegalArgumentException("Product is not in the stock, Try again Later");
        }
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
