package com.bakery.bakeryplatform.controller.website;

import com.bakery.bakeryplatform.service.catalogue.SiteCatalogue;
import com.bakery.bakeryplatform.service.user.SiteIdentity;

import com.bakery.bakeryplatform.model.CartItem;
import com.bakery.bakeryplatform.repository.CartItemRepository;
import com.bakery.bakeryplatform.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.*;

@RestController @RequestMapping("/api/site/cart")
public class SiteCartController {
    private final CartService cart;private final CartItemRepository rows;private final SiteCatalogue products;private final SiteIdentity identity;
    public SiteCartController(CartService cart,CartItemRepository rows,SiteCatalogue products,SiteIdentity identity){this.cart=cart;this.rows=rows;this.products=products;this.identity=identity;}
    private static Map<String, Object> dto(CartItem item) {
        return map("id", item.getId(), "cartItemId", item.getId(),
                "productId", SiteCatalogue.baseProductId(item.getProductId()),
                "productName", item.getProductName(), "productType", item.getProductType(),
                "selectedSize", item.getSelectedSize(), "quantity", item.getQuantity(),
                "unitPrice", item.getUnitPrice(), "subtotal", item.getTotal(), "imageUrl", item.getImageUrl());
    }
    @GetMapping public Map<String,Object> read(HttpSession session){var result=cart.getCart(identity.cartOwner(session),null);return map("items",result.items().stream().map(SiteCartController::dto).toList(),"total",result.total());}
    @PostMapping({"","/add"}) public Map<String,Object> add(@RequestBody Map<String,Object> input,HttpSession session){
        String productId=text(input,"productId",text(input,"id",""));var p=products.find(productId);
        String size=text(input,"selectedSize",products.cake(p)?"1 kg":"Regular");int quantity=(int)SiteCatalogue.number(input.getOrDefault("quantity",1));
        if(quantity<1||quantity>50)throw error(HttpStatus.BAD_REQUEST,"Choose a quantity from 1 to 50.");
        Long owner=identity.cartOwner(session);var c=new CartItem(owner,p.getId()+"::"+size,p.getName(),quantity,products.price(p,size));
        c.setSelectedSize(size);c.setProductType(products.cake(p)?"CAKE":"BAKERY");c.setImageUrl(p.getImageUrl());
        CartItem saved=cart.addToCart(c);saved.setUnitPrice(c.getUnitPrice());rows.save(saved);return map("success",true,"item",dto(saved));
    }
    private CartItem owned(Long id,HttpSession session){var c=rows.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Cart item not found."));if(!c.getUserId().equals(identity.cartOwner(session)))throw error(HttpStatus.FORBIDDEN,"This is not your cart.");return c;}
    @PostMapping("/{id}/update") public Map<String,Object> quantity(@PathVariable Long id,@RequestParam int quantity,HttpSession session){owned(id,session);if(quantity<1||quantity>50)throw error(HttpStatus.BAD_REQUEST,"Choose a quantity from 1 to 50.");return map("success",true,"item",dto(cart.updateQuantity(id,quantity)));}
    @PostMapping("/{id}/delete") public Map<String,Object> remove(@PathVariable Long id,HttpSession session){owned(id,session);cart.removeItem(id);return map("success",true);}
    @PostMapping("/clear") public Map<String,Object> clear(HttpSession session){cart.clearCart(identity.cartOwner(session));return map("success",true);}
}
