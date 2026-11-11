package com.bakery.bakeryplatform.service.catalogue;

import com.bakery.bakeryplatform.model.catalogue.SiteStock;
import com.bakery.bakeryplatform.repository.catalogue.SiteStockRepository;

import com.bakery.bakeryplatform.model.catalogue.Item;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.*;

@Service
public class SiteCatalogue {
    private final ItemService items;
    private final SiteStockRepository stocks;
    public SiteCatalogue(ItemService items,SiteStockRepository stocks){this.items=items;this.stocks=stocks;}

    public static String baseProductId(String cartProductId) {
        return cartProductId.split("::", 2)[0];
    }

    public List<Item> all(){return items.getAllItems();}
    @Transactional public void seed(Item product){
        items.saveWebsiteItem(product);
        if(!cake(product))stocks.save(new SiteStock(product.getId(),(int)number(product.getWebsite().getOrDefault("stockQuantity",50))));
    }
    @Transactional public void ensureStock(Item product){
        if(!cake(product)&&!stocks.existsById(product.getId()))stocks.save(new SiteStock(product.getId(),(int)number(product.getWebsite().getOrDefault("stockQuantity",50))));
    }
    public Item find(String id){return all().stream().filter(p->p.getId().equals(id)).findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"Product not found."));}
    public boolean cake(Item p){return "CAKE".equals(text(p.getWebsite(),"productType",p.getId().startsWith("CAKE-")?"CAKE":"BAKERY"));}
    public Map<String,Object> dto(Item p){
        Map<String,Object> d=new LinkedHashMap<>(p.getWebsite());
        d.putAll(map("id",p.getId(),"productId",p.getId(),"productCode",p.getId(),"cakeCode",p.getId(),"name",p.getName(),"price",p.getPrice(),"description",p.getDescription(),"category",p.getCategory(),"imageUrl",p.getImageUrl(),"imagePath",p.getImageUrl(),"available","Available".equals(p.getAvailability()),"productType",cake(p)?"CAKE":"BAKERY"));
        d.put("stockQuantity",stocks.findById(p.getId()).map(SiteStock::getQuantity).orElse(0));
        d.putIfAbsent("discountEnabled",false);d.putIfAbsent("discountPercentage",0);
        return d;
    }
    @Transactional
    public Map<String,Object> save(Map<String,Object> input,boolean create){
        String id=text(input,"productId",text(input,"id",""));
        if(create)id=("CAKE".equals(text(input,"productType","BAKERY"))?"CAKE-":"PROD-")+UUID.randomUUID();
        Item p=create?new Item():find(id);
        String name=text(input,"name",p.getName()==null?"":p.getName());
        String category=text(input,"category",p.getCategory()==null?"":p.getCategory());
        double price=number(input.getOrDefault("price",p.getPrice()));
        if(name.isBlank()||name.length()>100||category.isBlank()||price<=0)throw error(HttpStatus.BAD_REQUEST,"Name, category and a positive price are required.");
        p.setId(id);p.setName(name);p.setCategory(category);p.setPrice(price);
        p.setDescription(text(input,"description",name));p.setFlavor(text(input,"flavor","Classic"));p.setSize("Regular");p.setShape(0);
        p.setImageUrl(text(input,"imageUrl",text(input,"imagePath","/images/placeholder.svg")));
        p.setAvailability(Boolean.FALSE.equals(input.get("available"))?"Unavailable":"Available");
        Map<String,Object> meta=new LinkedHashMap<>(p.getWebsite());meta.putAll(input);p.setWebsite(meta);
        double percent=number(meta.getOrDefault("discountPercentage",0));
        if(percent<0||percent>=100)throw error(HttpStatus.BAD_REQUEST,"Discount must be at least 0 and below 100 percent.");
        for(String field:List.of("price1kg","price15kg","price2kg"))if(meta.containsKey(field)&&number(meta.get(field))<=0)throw error(HttpStatus.BAD_REQUEST,"Enabled cake prices must be positive.");
        double requestedStock=number(input.getOrDefault("stockQuantity",stocks.findById(id).map(SiteStock::getQuantity).orElse(50)));
        if(!cake(p)&&(requestedStock<0||requestedStock>1000000||requestedStock!=Math.floor(requestedStock)))throw error(HttpStatus.BAD_REQUEST,"Stock must be a whole number from 0 to 1000000.");
        items.saveWebsiteItem(p);
        if(!cake(p)){
            int quantity=(int)requestedStock;
            SiteStock stock=stocks.findById(id).orElse(new SiteStock(id,quantity));stock.setQuantity(quantity);stocks.save(stock);
        }
        return dto(p);
    }
    public void delete(String id){find(id);items.deleteItemById(id);stocks.deleteById(id);}
    public BigDecimal price(Item p,String size){
        if(!"Available".equals(p.getAvailability()))throw error(HttpStatus.BAD_REQUEST,"This product is unavailable.");
        BigDecimal amount=BigDecimal.valueOf(p.getPrice());
        if(cake(p)){
            String field=size.startsWith("2")?"price2kg":size.startsWith("1.5")?"price15kg":"price1kg";
            String enabled=field.equals("price2kg")?"weight2kgEnabled":field.equals("price15kg")?"weight15kgEnabled":"weight1kgEnabled";
            if(Boolean.FALSE.equals(p.getWebsite().get(enabled)))throw error(HttpStatus.BAD_REQUEST,"This cake weight is unavailable.");
            double fallback=p.getPrice()+(field.equals("price2kg")?2000:field.equals("price15kg")?1000:0);
            amount=BigDecimal.valueOf(number(p.getWebsite().getOrDefault(field,fallback)));
        }
        if(Boolean.TRUE.equals(p.getWebsite().get("discountEnabled")))amount=amount.multiply(BigDecimal.ONE.subtract(BigDecimal.valueOf(number(p.getWebsite().getOrDefault("discountPercentage",0))).movePointLeft(2)));
        return amount.setScale(2,RoundingMode.HALF_UP);
    }
    public static double number(Object value){try{double n=Double.parseDouble(value.toString());if(!Double.isFinite(n))throw new NumberFormatException();return n;}catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"Enter a valid number.");}}
}
