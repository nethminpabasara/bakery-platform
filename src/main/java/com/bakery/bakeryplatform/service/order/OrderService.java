package com.bakery.bakeryplatform.service.order;

import com.bakery.bakeryplatform.exception.BusinessRuleException;
import com.bakery.bakeryplatform.exception.ResourceNotFoundException;
import com.bakery.bakeryplatform.model.CartItem;
import com.bakery.bakeryplatform.model.order.Order;
import com.bakery.bakeryplatform.model.order.OrderStatus;
import com.bakery.bakeryplatform.repository.CartItemRepository;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;

    public OrderService(OrderRepository orderRepository, CartItemRepository cartItemRepository) {
        this.orderRepository = orderRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // Create: place an order from the cart, then empty the cart
    @Transactional
    public Order placeOrder(Long userId, String note) {
        List<CartItem> items = cartItemRepository.findByUserId(userId);
        if (items.isEmpty()) {
            throw new BusinessRuleException("Your cart is empty.");
        }

        BigDecimal total = BigDecimal.ZERO;
        StringBuilder summary = new StringBuilder();

        for (CartItem item : items) {
            checkAvailability(item);
            total = total.add(item.getTotal());

            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(item.getQuantity()).append(" x ").append(item.getProductName());
        }

        Order order = new Order(userId, note, summary.toString(), total);
        Order saved = orderRepository.save(order);

        cartItemRepository.deleteByUserId(userId);   // the cart is now empty
        return saved;
    }

    // Read
    public List<Order> getAll(OrderStatus status) {
        if (status == null) {
            return orderRepository.findAll();
        }
        return orderRepository.findByStatus(status);
    }

    public List<Order> getByUser(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order getById(Long id) {
        return findOrder(id);
    }

    // Update change the status (only one step forward)
    public Order updateStatus(Long id, OrderStatus newStatus) {
        Order order = findOrder(id);
        if (!order.getStatus().canMoveTo(newStatus)) {
            throw new BusinessRuleException(
                    "An order cannot move from " + order.getStatus() + " to " + newStatus + ".");
        }
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // Update edit the note (only before preparation starts)
    public Order editNote(Long id, String note) {
        Order order = findOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Only pending orders can be edited.");
        }
        order.setNote(note);
        return orderRepository.save(order);
    }

    // Delete (only while pending)
    public void cancel(Long id) {
        Order order = findOrder(id);
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Only pending orders can be cancelled.");
        }
        orderRepository.delete(order);
    }


    // table if stockQuantity >= item.getQuantity().
    private void checkAvailability(CartItem item) {
        if (item.getQuantity() < 1) {
            throw new BusinessRuleException(item.getProductName() + " has an invalid quantity.");
        }
    }

    private Order findOrder(Long id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            throw new ResourceNotFoundException("Order not found with id " + id);
        }
        return order;
    }
}