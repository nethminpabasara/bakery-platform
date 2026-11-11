package com.bakery.bakeryplatform.controller.website;

import com.bakery.bakeryplatform.service.cake.SiteBookings;
import com.bakery.bakeryplatform.service.catalogue.SiteCatalogue;
import com.bakery.bakeryplatform.service.user.SiteIdentity;

import com.bakery.bakeryplatform.model.cake.BookingStatus;
import com.bakery.bakeryplatform.model.cake.CakeDesign;
import com.bakery.bakeryplatform.model.cake.CakeOption;
import com.bakery.bakeryplatform.model.cake.Flavour;
import com.bakery.bakeryplatform.model.cake.Size;
import com.bakery.bakeryplatform.model.cake.Topping;
import com.bakery.bakeryplatform.repository.cake.CakeDesignRepository;
import com.bakery.bakeryplatform.repository.cake.CakeOptionRepository;
import com.bakery.bakeryplatform.repository.cake.CustomCakeBookingRepository;
import com.bakery.bakeryplatform.controller.cake.CakeDesignController;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.*;

@RestController @RequestMapping("/api/site")
public class SiteCakeController {
    private final SiteIdentity identity;private final SiteBookings site;private final CustomCakeBookingRepository bookings;private final CakeDesignRepository designs;private final CakeOptionRepository options;private final CakeDesignController uploads;
    public SiteCakeController(SiteIdentity identity,SiteBookings site,CustomCakeBookingRepository bookings,CakeDesignRepository designs,CakeOptionRepository options,CakeDesignController uploads){this.identity=identity;this.site=site;this.bookings=bookings;this.designs=designs;this.options=options;this.uploads=uploads;}
    @GetMapping({"/cakeBookings","/bookings/my","/account/bookings"}) public List<Map<String,Object>> bookings(HttpSession session){Long owner=identity.admin(session)?null:identity.customer(session).getId();return bookings.findAll().stream().filter(b->owner==null||owner.equals(b.getUserId())).map(site::dto).toList();}
    @PostMapping("/bookings/quote") public Map<String,Object> quote(@RequestBody Map<String,Object> input){return map("estimatedPrice",site.quote(input));}
    @PutMapping("/bookings/{id}") public Map<String,Object> edit(@PathVariable Long id,@RequestBody Map<String,Object> input,HttpSession session){return map("success",true,"booking",site.dto(site.save(input,site.owned(id,session),session)));}
    @DeleteMapping("/bookings/{id}") public Map<String,Object> cancel(@PathVariable Long id,HttpSession session){var b=site.owned(id,session);if(!identity.admin(session)&&b.getStatus()!=BookingStatus.PENDING)throw error(HttpStatus.CONFLICT,"Only pending bookings can be cancelled.");b.setStatus(BookingStatus.CANCELLED);bookings.save(b);return map("success",true);}
    @PostMapping("/admin/updateBookingStatus") public Map<String,Object> status(@RequestBody Map<String,Object> input,HttpSession session){identity.requireAdmin(session);var b=site.owned(id(input.get("bookingId").toString()),session);try{b.setStatus(BookingStatus.valueOf(text(input,"status","").toUpperCase()));}catch(Exception e){throw error(HttpStatus.BAD_REQUEST,"Invalid booking status.");}bookings.save(b);return map("success",true);}
    private Map<String,Object> design(CakeDesign d){return map("id",d.getId(),"designCode","DES-"+d.getId(),"shape",d.getShape(),"flavor",d.getFlavor(),"decoration",d.getDecoration(),"filling",d.getFilling(),"colour",d.getColour(),"layers",d.getLayers(),"size",d.getCakeSize(),"message",d.getCakeMessage(),"cakeMessage",d.getCakeMessage(),"referenceImagePath",d.getReferenceImageUrl(),"referenceImageUrl",d.getReferenceImageUrl(),"estimatedPrice",d.getEstimatedPrice());}
    @GetMapping("/cake-designs") public List<Map<String,Object>> designs(HttpSession session){Long user=identity.customer(session).getId();return designs.findAll().stream().filter(d->user.equals(d.getUserId())).map(this::design).toList();}
    @PostMapping("/cake-designs") public Map<String,Object> saveDesign(@RequestBody Map<String,Object> input,HttpSession session){var d=new CakeDesign();d.setUserId(identity.customer(session).getId());return map("success",true,"designCode","DES-"+updateDesign(d,input).getId());}
    private CakeDesign ownedDesign(Long id,HttpSession session){var d=designs.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Design not found."));identity.owner(d.getUserId(),session);return d;}
    @PutMapping("/cake-designs/{id}") public Map<String,Object> editDesign(@PathVariable Long id,@RequestBody Map<String,Object> input,HttpSession session){return map("success",true,"design",design(updateDesign(ownedDesign(id,session),input)));}
    private CakeDesign updateDesign(CakeDesign d,Map<String,Object> input){d.setShape(text(input,"shape",d.getShape()==null?"Round":d.getShape()));d.setFlavor(text(input,"flavor",d.getFlavor()==null?"Vanilla":d.getFlavor()));d.setDecoration(text(input,"decoration",d.getDecoration()==null?"Strawberry":d.getDecoration()));d.setFilling(text(input,"filling",d.getFilling()));d.setCakeMessage(text(input,"cakeMessage",text(input,"message",d.getCakeMessage())));d.setReferenceImageUrl(text(input,"referenceImageUrl",text(input,"referenceImagePath",d.getReferenceImageUrl())));d.setColour(text(input,"colour",d.getColour()));d.setCakeSize(text(input,"size",d.getCakeSize()));d.setLayers((int)SiteCatalogue.number(input.getOrDefault("layers",1)));if(d.getLayers()<1||d.getLayers()>10)throw error(HttpStatus.BAD_REQUEST,"Choose 1 to 10 layers.");d.setEstimatedPrice(site.quote(map("shape",d.getShape(),"flavor",d.getFlavor(),"decoration",d.getDecoration(),"filling",d.getFilling(),"cakeSize",d.getCakeSize())));return designs.save(d);}
    @DeleteMapping("/cake-designs/{id}") public Map<String,Object> deleteDesign(@PathVariable Long id,HttpSession session){designs.delete(ownedDesign(id,session));return map("success",true);}
    @PostMapping("/cake-designs/upload") public Map<String,Object> upload(@RequestParam MultipartFile file,HttpSession session)throws Exception{identity.customer(session);return uploads.uploadReferenceImage(file);}
    private Map<String,Object> option(CakeOption o){return map("id",o.getId(),"optionType",o.getOptionType(),"type",o.getOptionType(),"name",o.getName(),"extraPrice",o.getExtraPrice(),"price",o.getExtraPrice(),"available",o.isAvailable(),"active",o.isAvailable());}
    @GetMapping({"/cake-options","/cake-options/available"}) public List<Map<String,Object>> options(jakarta.servlet.http.HttpServletRequest request){boolean available=request.getRequestURI().endsWith("/available");return options.findAll().stream().filter(o->!available||o.isAvailable()).map(this::option).toList();}
    @PostMapping("/cake-options") public Map<String,Object> addOption(@RequestBody Map<String,Object> input,HttpSession session){identity.requireAdmin(session);String type=text(input,"optionType",text(input,"type",""));CakeOption o=switch(type){case "FLAVOUR","FLAVOR"->new Flavour();case "SIZE","SHAPE"->new Size();case "TOPPING","DECORATION"->new Topping();default->throw error(HttpStatus.BAD_REQUEST,"Choose a valid cake option type.");};return saveOption(o,input);}
    @PutMapping("/cake-options/{id}") public Map<String,Object> editOption(@PathVariable Long id,@RequestBody Map<String,Object> input,HttpSession session){identity.requireAdmin(session);return saveOption(options.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Option not found.")),input);}
    private Map<String,Object> saveOption(CakeOption o,Map<String,Object> input){String name=text(input,"name",o.getName()==null?"":o.getName());double extra=SiteCatalogue.number(input.getOrDefault("extraPrice",input.getOrDefault("price",0)));if(name.isBlank()||extra<0)throw error(HttpStatus.BAD_REQUEST,"A name and non-negative extra price are required.");o.setName(name);o.setConfiguredExtraPrice(extra);o.setAvailable(!Boolean.FALSE.equals(input.getOrDefault("available",true)));options.save(o);return map("success",true,"option",option(o));}
    @DeleteMapping("/cake-options/{id}") public Map<String,Object> deleteOption(@PathVariable Long id,HttpSession session){identity.requireAdmin(session);if(!options.existsById(id))throw error(HttpStatus.NOT_FOUND,"Option not found.");options.deleteById(id);return map("success",true);}
}
