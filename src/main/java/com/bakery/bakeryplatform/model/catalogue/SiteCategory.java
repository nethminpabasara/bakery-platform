package com.bakery.bakeryplatform.model.catalogue;

import jakarta.persistence.*;

@Entity
@Table(name="website_categories", uniqueConstraints=@UniqueConstraint(columnNames={"name","section"}))
public class SiteCategory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    private String name;
    @Column(length=1000) private String description;
    private String section;
    private boolean active=true;
    public Long getId(){return id;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getSection(){return section;} public void setSection(String v){section=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
