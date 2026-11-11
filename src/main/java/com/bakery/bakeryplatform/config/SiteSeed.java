package com.bakery.bakeryplatform.config;

import com.bakery.bakeryplatform.model.catalogue.SiteCategory;
import com.bakery.bakeryplatform.repository.catalogue.SiteCategoryRepository;
import com.bakery.bakeryplatform.service.catalogue.SiteCatalogue;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;
import java.util.*;

@Component
@ConditionalOnProperty(name="app.website.seed-catalogue",havingValue="true",matchIfMissing=true)
public class SiteSeed implements ApplicationRunner {
    private final SiteCatalogue products;private final SiteCategoryRepository categories;private final ObjectMapper mapper;private final com.bakery.bakeryplatform.repository.cake.CakeOptionRepository options;
    public SiteSeed(SiteCatalogue products,SiteCategoryRepository categories,ObjectMapper mapper,com.bakery.bakeryplatform.repository.cake.CakeOptionRepository options){this.products=products;this.categories=categories;this.mapper=mapper;this.options=options;}
    @Override public void run(ApplicationArguments args)throws Exception{
        if(products.all().isEmpty())try(var source=getClass().getResourceAsStream("/website/catalogue.json")){
            List<Map<String,Object>> records=mapper.readValue(source,new TypeReference<>(){});
            // Keep the frontend's existing catalogue codes, rather than inventing replacement IDs.
            for(Map<String,Object> record:records){
                var p=new com.bakery.bakeryplatform.model.catalogue.Item();p.setId(record.get("id").toString());
                p.setName(record.get("name").toString());p.setPrice(SiteCatalogue.number(record.get("price")));p.setCategory(record.get("category").toString());
                p.setDescription(record.get("description").toString());p.setFlavor("Classic");p.setSize("Regular");p.setShape(0);p.setAvailability("Available");p.setImageUrl(record.get("imageUrl").toString());p.setWebsite(record);
                products.seed(p);
            }
        }
        products.all().forEach(products::ensureStock);
        if(options.count()==0){
            String[][] definitions={{"FLAVOUR","Vanilla","0"},{"FLAVOUR","Chocolate","400"},{"FLAVOUR","Red Velvet","600"},{"SHAPE","Round","0"},{"SHAPE","Square","100"},{"SHAPE","Heart","500"},{"SHAPE","Tiered","4000"},{"DECORATION","Strawberry","100"},{"DECORATION","Floral","0"},{"DECORATION","Chocolate Drip","200"}};
            for(String[] d:definitions){com.bakery.bakeryplatform.model.cake.CakeOption option=switch(d[0]){case "FLAVOUR"->new com.bakery.bakeryplatform.model.cake.Flavour();case "SHAPE"->new com.bakery.bakeryplatform.model.cake.Size();default->new com.bakery.bakeryplatform.model.cake.Topping();};option.setName(d[1]);option.setConfiguredExtraPrice(Double.valueOf(d[2]));options.save(option);}
        }
        if(categories.count()==0){
            Set<String> seen=new HashSet<>();for(var p:products.all()){
                String section=products.cake(p)?"CUSTOM_CAKE":"DAILY_BAKERY";
                if(!seen.add(section+":"+p.getCategory()))continue;
                var c=new SiteCategory();c.setName(p.getCategory());c.setSection(section);c.setDescription(p.getCategory());categories.save(c);
            }
        }
    }
}
