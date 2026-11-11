package com.bakery.bakeryplatform.controller.website;

import com.bakery.bakeryplatform.model.catalogue.SiteCategory;
import com.bakery.bakeryplatform.repository.catalogue.SiteCategoryRepository;
import com.bakery.bakeryplatform.service.catalogue.SiteCatalogue;
import com.bakery.bakeryplatform.service.user.SiteIdentity;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static com.bakery.bakeryplatform.service.user.SiteIdentity.*;

@RestController @RequestMapping("/api/site")
public class SiteCatalogueController {
    private final SiteCatalogue products;private final SiteCategoryRepository categories;private final SiteIdentity identity;
    public SiteCatalogueController(SiteCatalogue products,SiteCategoryRepository categories,SiteIdentity identity){this.products=products;this.categories=categories;this.identity=identity;}
    @GetMapping({"/products","/bakery-products","/cakes","/cakes/birthday","/cakes/recommend"})
    public List<Map<String,Object>> list(jakarta.servlet.http.HttpServletRequest request,HttpSession session){
        String path=request.getRequestURI();boolean cakes=path.contains("/cakes");boolean bakery=path.endsWith("/bakery-products");
        var result=products.all().stream().filter(p->!cakes||products.cake(p)).filter(p->!bakery||!products.cake(p))
            .filter(p->!path.endsWith("/birthday")||Boolean.TRUE.equals(p.getWebsite().get("birthdayCake")))
            .filter(p->identity.admin(session)||categories.findAll().stream().noneMatch(c->c.getName().equalsIgnoreCase(p.getCategory())&&!c.isActive()))
            .map(products::dto).toList();
        if(path.endsWith("/recommend")){
            result=result.stream().filter(p->request.getParameter("shape")==null||request.getParameter("shape").equalsIgnoreCase(String.valueOf(p.get("shape"))))
                .filter(p->request.getParameter("flavor")==null||request.getParameter("flavor").equalsIgnoreCase(String.valueOf(p.get("flavor")))).limit(6).toList();
        }
        return result;
    }
    @GetMapping("/products/{id}") public Map<String,Object> product(@PathVariable String id){return products.dto(products.find(id));}
    @PostMapping({"/products","/products/update"}) public Map<String,Object> save(@RequestBody Map<String,Object> data,HttpSession session,jakarta.servlet.http.HttpServletRequest request){
        identity.requireAdmin(session);var product=products.save(data,!request.getRequestURI().endsWith("/update"));return map("success",true,"product",product);
    }
    @PostMapping("/products/delete") public Map<String,Object> delete(@RequestParam String productId,HttpSession session){identity.requireAdmin(session);products.delete(productId);return map("success",true);}
    @GetMapping("/categories") public List<Map<String,Object>> categories(@RequestParam(required=false) String section,@RequestParam(defaultValue="false") boolean activeOnly){return categories.findAll().stream().filter(c->section==null||section.equals(c.getSection())).filter(c->!activeOnly||c.isActive()).map(this::category).toList();}
    private Map<String,Object> category(SiteCategory c){return map("id",c.getId(),"categoryId",c.getId().toString(),"name",c.getName(),"description",c.getDescription(),"section",c.getSection(),"active",c.isActive(),"productCount",products.all().stream().filter(p->p.getCategory().equalsIgnoreCase(c.getName())).count());}
    @PostMapping("/categories") public Map<String,Object> createCategory(@RequestBody Map<String,Object> data,HttpSession session){identity.requireAdmin(session);return category(saveCategory(new SiteCategory(),data));}
    @PutMapping("/categories/{id}") public Map<String,Object> updateCategory(@PathVariable Long id,@RequestBody Map<String,Object> data,HttpSession session){identity.requireAdmin(session);return category(saveCategory(categories.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Category not found.")),data));}
    private SiteCategory saveCategory(SiteCategory c,Map<String,Object> data){
        String name=text(data,"name",c.getName()==null?"":c.getName());String section=text(data,"section",c.getSection()==null?"DAILY_BAKERY":c.getSection());
        if(name.isBlank()||name.length()>100||!Set.of("DAILY_BAKERY","CUSTOM_CAKE").contains(section))throw error(HttpStatus.BAD_REQUEST,"A category name and valid section are required.");
        if(categories.findAll().stream().anyMatch(other->!Objects.equals(c.getId(),other.getId())&&other.getName().equalsIgnoreCase(name)&&other.getSection().equals(section)))throw error(HttpStatus.CONFLICT,"This category already exists.");
        c.setName(name);c.setSection(section);c.setDescription(text(data,"description",""));if(data.containsKey("active"))c.setActive(Boolean.parseBoolean(data.get("active").toString()));return categories.save(c);
    }
    @DeleteMapping("/categories/{id}") public Map<String,Object> deleteCategory(@PathVariable Long id,HttpSession session){identity.requireAdmin(session);var c=categories.findById(id).orElseThrow(()->error(HttpStatus.NOT_FOUND,"Category not found."));if(products.all().stream().anyMatch(p->p.getCategory().equalsIgnoreCase(c.getName())))throw error(HttpStatus.CONFLICT,"Move this category's products before deleting it.");categories.delete(c);return map("success",true);}
    @PostMapping("/promo/validate") public Map<String,Object> promo(){throw error(HttpStatus.BAD_REQUEST,"No promotional code is currently active.");}
}
