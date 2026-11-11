package com.bakery.bakeryplatform.service.cake;

import com.bakery.bakeryplatform.model.cake.CakeDesign;
import com.bakery.bakeryplatform.repository.cake.CakeDesignRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CakeDesignService {

    @Autowired
    private CakeDesignRepository repository;

    public CakeDesign saveDesign(CakeDesign design) {
        return repository.save(design);
    }

    public List<CakeDesign> getAllDesigns() {
        return repository.findAll();
    }

    public CakeDesign updateDesign(Long id, CakeDesign updatedDesign) {
        CakeDesign existing = repository.findById(id).orElse(null);
        if (existing != null) {
            existing.setShape(updatedDesign.getShape());
            existing.setFlavor(updatedDesign.getFlavor());
            existing.setDecoration(updatedDesign.getDecoration());
            existing.setFilling(updatedDesign.getFilling());
            existing.setCakeMessage(updatedDesign.getCakeMessage());
            existing.setEstimatedPrice(updatedDesign.getEstimatedPrice());
            return repository.save(existing);
        }
        return null;
    }

    public void deleteDesign(Long id) {
        repository.deleteById(id);
    }
}