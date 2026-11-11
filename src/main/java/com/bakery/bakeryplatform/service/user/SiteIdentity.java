package com.bakery.bakeryplatform.service.user;

import com.bakery.bakeryplatform.exception.ResourceNotFoundException;
import com.bakery.bakeryplatform.model.order.Order;
import com.bakery.bakeryplatform.model.user.AdminUser;
import com.bakery.bakeryplatform.model.user.Customer;
import com.bakery.bakeryplatform.repository.order.OrderRepository;
import com.bakery.bakeryplatform.repository.user.AdminUserRepository;
import com.bakery.bakeryplatform.repository.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class SiteIdentity {
    private final UserRepository users;
    private final AdminUserRepository admins;
    private final OrderRepository orders;

    public SiteIdentity(UserRepository users, AdminUserRepository admins, OrderRepository orders) {
        this.users = users;
        this.admins = admins;
        this.orders = orders;
    }

    public Customer customer(HttpSession session) {
        return customer(session, "Please sign in as a customer.");
    }

    public Long requireCustomerId(HttpSession session) {
        return customer(session, "Customer login required.").getId();
    }

    private Customer customer(HttpSession session, String message) {
        Object id = session.getAttribute("loggedInUserId");
        if (id instanceof Long value && users.findById(value).orElse(null) instanceof Customer customer) {
            return customer;
        }
        throw error(HttpStatus.UNAUTHORIZED, message);
    }

    public void requirePaymentUser(Long userId, HttpSession session) {
        if (!admin(session) && !requireCustomerId(session).equals(userId)) {
            throw error(HttpStatus.FORBIDDEN, "This payment belongs to another customer.");
        }
    }

    public void requirePaymentOrder(Long orderId, HttpSession session) {
        if (admin(session)) {
            return;
        }
        Long customerId = requireCustomerId(session);
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found."));
        if (!customerId.equals(order.getUserId())) {
            throw error(HttpStatus.FORBIDDEN, "This payment belongs to another customer.");
        }
    }

    public boolean admin(HttpSession session) {
        Object id = session.getAttribute("loggedInAdminId");
        return id instanceof Long value && admins.existsById(value);
    }
    public void requireAdmin(HttpSession session){if(!admin(session))throw error(HttpStatus.UNAUTHORIZED,"Administrator login required.");}
    public void owner(Long owner,HttpSession session){if(!admin(session)&&!customer(session).getId().equals(owner))throw error(HttpStatus.FORBIDDEN,"This record belongs to another customer.");}
    public Long cartOwner(HttpSession session){
        Object id=session.getAttribute("loggedInUserId");
        if(id instanceof Long value && users.existsById(value))return value;
        Object guest=session.getAttribute("websiteGuestCartId");
        if(guest instanceof Long value)return value;
        Long value=-ThreadLocalRandom.current().nextLong(1,Long.MAX_VALUE);
        session.setAttribute("websiteGuestCartId",value);return value;
    }
    public Map<String,Object> user(Customer c){return map("id",c.getId(),"userId",c.getId().toString(),"fullName",c.getUsername(),"username",c.getUsername(),"email",c.getEmail(),"phone",c.getPhoneNumber(),"address",c.getAddress(),"role","CUSTOMER");}
    public Map<String,Object> administrator(AdminUser a){return map("id",a.getId(),"userId","ADMIN-"+a.getId(),"fullName",a.getUsername(),"email",a.getEmail(),"role","ADMIN");}
    public static Map<String,Object> map(Object... entries){Map<String,Object> result=new LinkedHashMap<>();for(int i=0;i<entries.length;i+=2)result.put(entries[i].toString(),entries[i+1]);return result;}
    public static ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
    public static String text(Map<String,?> data,String key,String fallback){Object value=data.get(key);return value==null?fallback:value.toString().trim();}
    public static Long id(String value){try{return Long.valueOf(value);}catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"Invalid record ID.");}}
}
