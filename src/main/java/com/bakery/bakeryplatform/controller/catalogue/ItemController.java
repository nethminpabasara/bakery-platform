package com.bakery.bakeryplatform.controller.catalogue;


import com.bakery.bakeryplatform.model.catalogue.Item;
import com.bakery.bakeryplatform.service.catalogue.ItemPopularityService;
import com.bakery.bakeryplatform.service.catalogue.ItemService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

@Controller
public class ItemController {
    private final ItemService itemService;
    private final ItemPopularityService itemPopularityService;

    public ItemController(ItemService itemService, ItemPopularityService itemPopularityService) {
        this.itemService = itemService;
        this.itemPopularityService = itemPopularityService;
    }

    @GetMapping(value = "/api/items", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Item>> getStorefrontItems() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(itemService.getAllItems());
    }

    @GetMapping(value = "/api/items/recommended", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Item>> getRecommendedItems() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(itemPopularityService.getPopularItems(itemService.getAllItems(), 4));
    }

    @PostMapping(value = "/api/items/{id}/cart-click", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Void> recordAddToCart(@PathVariable String id) {
        boolean availableItem = itemService.getAllItems().stream()
                .anyMatch(candidate -> id.equals(candidate.getId())
                        && "Available".equals(candidate.getAvailability()));
        if (!availableItem) {
            return ResponseEntity.notFound().build();
        }
        itemPopularityService.recordAddToCart(id);
        return ResponseEntity.noContent().build();
    }

    // Handle POST requests for adding a new item
    @PostMapping("/admin/items/upload")
    public String addItem(
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String flavor,
            @RequestParam String size,
            @RequestParam double price,
            @RequestParam int shape,
            @RequestParam String category,
            @RequestParam String availability,
            @RequestParam MultipartFile imageFile,
            RedirectAttributes redirectAttributes) throws IOException {
        try {
            itemService.addItem(name, description, flavor, size, price, shape,
                    category, availability, imageFile);
            redirectAttributes.addFlashAttribute("itemMessage", "Item added successfully.");
            redirectAttributes.addFlashAttribute("itemMessageType", "success");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("itemMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("itemMessageType", "error");
        }
        return "redirect:/admin.html?tab=products";
    }

    // Handle POST requests for updating an existing item
    @PostMapping("/admin/items/update")
    public String updateItem(
            @RequestParam String id,
            @RequestParam String name,
            @RequestParam String description,
            @RequestParam String flavor,
            @RequestParam String size,
            @RequestParam double price,
            @RequestParam int shape,
            @RequestParam String category,
            @RequestParam String availability,
            @RequestParam(required = false) MultipartFile imageFile,
            RedirectAttributes redirectAttributes) throws IOException {
        try {
            itemService.updateItem(id, name, description, flavor, size, price, shape,
                    category, availability, imageFile);
            redirectAttributes.addFlashAttribute("itemMessage", "Item updated successfully.");
            redirectAttributes.addFlashAttribute("itemMessageType", "success");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("itemMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("itemMessageType", "error");
        }
        return "redirect:/admin.html?tab=products";
    }

    // Handle POST requests for deleting an item
    @PostMapping("/admin/items/delete")
    public String deleteItem(@RequestParam String id, RedirectAttributes redirectAttributes) {
        try {
            itemService.deleteItemById(id);
            redirectAttributes.addFlashAttribute("itemMessage", "Item deleted successfully.");
            redirectAttributes.addFlashAttribute("itemMessageType", "success");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("itemMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("itemMessageType", "error");
        }
        return "redirect:/admin.html?tab=products";
    }

    // Handle GET requests for retrieving item images
    @GetMapping("/item-images/{filename:.+}")
    public ResponseEntity<Resource> getItemImage(@PathVariable String filename) throws IOException {
        Path imagePath;
        try {
            imagePath = itemService.getImagePath(filename);
        } catch (NoSuchFileException e) {
            return ResponseEntity.notFound().build();
        }
        String contentType = Files.probeContentType(imagePath);
        MediaType mediaType = contentType == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(contentType);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(mediaType)
                .body(new FileSystemResource(imagePath));
    }
}
