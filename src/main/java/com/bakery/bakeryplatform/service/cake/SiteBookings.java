package com.bakery.bakeryplatform.service.cake;

import com.bakery.bakeryplatform.service.catalogue.SiteCatalogue;
import com.bakery.bakeryplatform.service.user.SiteIdentity;

import com.bakery.bakeryplatform.model.cake.BookingStatus;
import com.bakery.bakeryplatform.model.cake.CustomCakeBooking;
import com.bakery.bakeryplatform.repository.cake.CakeOptionRepository;
import com.bakery.bakeryplatform.repository.cake.CustomCakeBookingRepository;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import java.time.*;
import java.util.*;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.*;

@Service
public class SiteBookings {
    private final SiteIdentity identity;private final BookingService service;private final CustomCakeBookingRepository rows;private final SiteCatalogue products;private final CakeOptionRepository options;
    public SiteBookings(SiteIdentity identity,BookingService service,CustomCakeBookingRepository rows,SiteCatalogue products,CakeOptionRepository options){this.identity=identity;this.service=service;this.rows=rows;this.products=products;this.options=options;}
    public CustomCakeBooking owned(Long id,HttpSession session){var b=rows.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Booking not found."));identity.owner(b.getUserId(),session);return b;}
    public Map<String,Object> dto(CustomCakeBooking b){return map("id",b.getBookingId(),"bookingId",b.getBookingId()==null?null:b.getBookingId().toString(),"userId",b.getUserId(),"customerName",b.getCustomerName(),"contactNumber",b.getContactNumber(),"email",b.getCustomerEmail(),"customerEmail",b.getCustomerEmail(),"requiredDate",b.getRequiredDate(),"preferredTime",b.getPreferredTime(),"occasion",b.getOccasion(),"shape",b.getShape(),"flavor",b.getFlavor(),"decoration",b.getDecoration(),"filling",b.getFilling(),"cakeSize",b.getCakeSize(),"servings",b.getServings(),"cakeMessage",b.getCakeMessage(),"pickupOrDelivery",b.getPickupOrDelivery(),"deliveryAddress",b.getDeliveryAddress(),"referenceImageUrl",b.getReferenceImageUrl(),"estimatedPrice",b.getEstimatedPrice(),"notes",b.getNotes(),"status",b.getStatus().name(),"paymentStatus","Awaiting bakery approval");}
    public double quote(Map<String,?> input){
        String shape=text(input,"shape","Round"),flavor=text(input,"flavor","Vanilla"),decoration=text(input,"decoration","Strawberry");
        double base=products.all().stream().filter(products::cake)
            .filter(p->text(p.getWebsite(),"shape","Round").equalsIgnoreCase(shape)&&text(p.getWebsite(),"flavor","Vanilla").equalsIgnoreCase(flavor)&&text(p.getWebsite(),"decoration","Strawberry").equalsIgnoreCase(decoration))
            .mapToDouble(p->p.getPrice()).findFirst().orElse(4600);
        // Existing catalogue prices already include the seed options. Apply changes
        // through the reviewed option subclasses' polymorphic pricing methods.
        base+=optionAdjustment("FLAVOUR",flavor)+optionAdjustment("SHAPE",shape)+optionAdjustment("DECORATION",decoration);
        String size=text(input,"cakeSize",text(input,"size","1.5 kg"));
        if(size.equals("1 kg")||size.equals("Small"))base=Math.max(3800,base-400);
        else if(size.equals("2 kg")||size.equals("Large"))base+=3500;
        else if(size.equals("Custom Servings")){int n=(int)SiteCatalogue.number(input.getOrDefault("servings",null)==null?35:input.get("servings"));if(n<4||n>250)throw error(HttpStatus.BAD_REQUEST,"Choose 4 to 250 servings.");base=4500+n*250;}
        if(text(input,"filling","").equals("Chocolate Ganache"))base+=300;
        if(text(input,"pickupOrDelivery","Pickup").equals("Delivery"))base+=600;
        return base;
    }
    private double optionAdjustment(String type,String name){
        var defaults=Map.of("Vanilla",0d,"Chocolate",400d,"Red Velvet",600d,"Round",0d,"Square",100d,"Heart",500d,"Tiered",4000d,"Strawberry",100d,"Floral",0d,"Chocolate Drip",200d);
        return options.findAll().stream().filter(o->o.getOptionType().equals(type)&&o.getName().equalsIgnoreCase(name)).findFirst().map(o->{if(!o.isAvailable())throw error(HttpStatus.CONFLICT,"This cake option is currently unavailable.");return o.getExtraPrice()-defaults.getOrDefault(o.getName(),0d);}).orElse(0d);
    }
    public void cancel(Long id,HttpSession session){var booking=owned(id,session);if(!identity.admin(session)&&booking.getStatus()!=BookingStatus.PENDING)throw error(HttpStatus.CONFLICT,"Only pending bookings can be cancelled.");booking.setStatus(BookingStatus.CANCELLED);rows.save(booking);}
    public CustomCakeBooking save(Map<String,?> input,CustomCakeBooking existing,HttpSession session){
        var customer=identity.admin(session)&&existing!=null?null:identity.customer(session);
        var b=existing==null?new CustomCakeBooking():existing;
        if(existing!=null&&!identity.admin(session)&&b.getStatus()!=BookingStatus.PENDING)throw error(HttpStatus.CONFLICT,"Only pending bookings can be edited.");
        if(customer!=null){b.setUserId(customer.getId());b.setCustomerEmail(customer.getEmail());}
        b.setCustomerName(text(input,"customerName",b.getCustomerName()==null?customer.getUsername():b.getCustomerName()));
        b.setContactNumber(text(input,"contactNumber",b.getContactNumber()==null?customer.getPhoneNumber():b.getContactNumber()));
        b.setRequiredDate(text(input,"requiredDate",b.getRequiredDate()==null?"":b.getRequiredDate()));
        LocalDate date;try{date=LocalDate.parse(b.getRequiredDate());}catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"Choose a valid cake collection date.");}
        if(date.isBefore(LocalDate.now(ZoneId.of("Asia/Colombo"))))throw error(HttpStatus.BAD_REQUEST,"The required date cannot be in the past.");
        b.setPreferredTime(text(input,"preferredTime",b.getPreferredTime()==null?"10:00":b.getPreferredTime()));
        b.setOccasion(text(input,"occasion",b.getOccasion()));b.setShape(text(input,"shape",b.getShape()==null?"Round":b.getShape()));b.setFlavor(text(input,"flavor",b.getFlavor()==null?"Vanilla":b.getFlavor()));
        b.setDecoration(text(input,"decoration",b.getDecoration()==null?"Strawberry":b.getDecoration()));b.setFilling(text(input,"filling",b.getFilling()==null?"Vanilla Cream":b.getFilling()));b.setCakeSize(text(input,"cakeSize",b.getCakeSize()==null?"1.5 kg":b.getCakeSize()));
        b.setCakeMessage(text(input,"cakeMessage",b.getCakeMessage()));b.setPickupOrDelivery(text(input,"pickupOrDelivery",b.getPickupOrDelivery()==null?"Pickup":b.getPickupOrDelivery()));b.setDeliveryAddress(text(input,"deliveryAddress",b.getDeliveryAddress()));
        b.setReferenceImageUrl(text(input,"referenceImageUrl",b.getReferenceImageUrl()));b.setNotes(text(input,"notes",b.getNotes()));
        if(b.getCustomerName()==null||b.getCustomerName().isBlank()||b.getContactNumber()==null||b.getContactNumber().isBlank())throw error(HttpStatus.BAD_REQUEST,"Customer name and phone are required.");
        if("Delivery".equals(b.getPickupOrDelivery())&&(b.getDeliveryAddress()==null||b.getDeliveryAddress().isBlank()))throw error(HttpStatus.BAD_REQUEST,"A delivery address is required.");
        if(input.get("servings")!=null&&!input.get("servings").toString().isBlank())b.setServings((int)SiteCatalogue.number(input.get("servings")));
        Map<String,Object> recipe=dto(b);b.setEstimatedPrice(quote(recipe));
        return existing==null?service.createBooking(b):rows.save(b);
    }
}
