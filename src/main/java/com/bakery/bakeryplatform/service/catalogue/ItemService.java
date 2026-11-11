package com.bakery.bakeryplatform.service.catalogue;


import com.bakery.bakeryplatform.model.catalogue.Item;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ItemService {
    /** Shared catalogue persistence for the supplied website's JSON product editor. */
    public synchronized Item saveWebsiteItem(Item product) {
        validateItem(product.getName(), product.getDescription(), product.getFlavor(), product.getSize(),
                product.getPrice(), product.getShape(), product.getCategory(), product.getAvailability());
        List<Item> products = new java.util.ArrayList<>(getAllItems());
        int index = -1;
        for (int i = 0; i < products.size(); i++) {
            if (Objects.equals(products.get(i).getId(), product.getId())) { index = i; break; }
        }
        if (index < 0) products.add(product); else products.set(index, product);
        saveItemsToFile(products);
        return product;
    }
    //Encapsulation
    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final TypeReference<List<Item>> ITEM_LIST_TYPE = new TypeReference<>() {};

    private final Path databaseFilePath;
    private final Path imageStoragePath;
    private final ObjectMapper objectMapper;

    //Constructor with @Autowired
    @Autowired
    public ItemService(
            @Value("${app.items.file:data/items.txt}") String databaseFile,
            @Value("${app.items.images:data/images}") String imageDirectory,
            ObjectMapper objectMapper) {
        this(Paths.get(databaseFile), Paths.get(imageDirectory), objectMapper);
    }

    //Constructor
    public ItemService(Path databaseFilePath, Path imageStoragePath, ObjectMapper objectMapper) {
        this.databaseFilePath = databaseFilePath.toAbsolutePath().normalize();
        this.imageStoragePath = imageStoragePath.toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
        initializeStorage();
    }

    public Path getDatabaseFilePath() {
        return databaseFilePath;
    }

    public Path getImageStoragePath() {
        return imageStoragePath;
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(databaseFilePath.getParent());
            Files.createDirectories(imageStoragePath);
            if (Files.notExists(databaseFilePath)) {
                copyExistingDatabase();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize item file storage.", e);
        }
    }

    //Copy existing database and Error handling
    private void copyExistingDatabase() throws IOException {
        Path oldJsonFile = Paths.get("src/main/resources/static/Item/data/items.json");
        Path oldTextFile = Paths.get("src/main/resources/static/Item/item.text");
        Path oldFile = Files.isRegularFile(oldTextFile) && Files.size(oldTextFile) > 0
                ? oldTextFile
                : oldJsonFile;

        if (Files.isRegularFile(oldFile) && Files.size(oldFile) > 0) {
            Files.copy(oldFile, databaseFilePath);
        } else {
            Files.writeString(databaseFilePath, "[]\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
    }

    //Get all items
    public synchronized List<Item> getAllItems() {
        try {
            String data = Files.readString(databaseFilePath, StandardCharsets.UTF_8);
            if (data.isBlank()) {
                return List.of();
            }
            List<Item> items = objectMapper.readValue(data, ITEM_LIST_TYPE);
            if (items == null) {
                throw new IllegalStateException("The item text file must contain a JSON array: " + databaseFilePath);
            }
            return items;
        } catch (JacksonException e) {
            throw new IllegalStateException("The item text file contains invalid JSON: " + databaseFilePath, e);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read items from " + databaseFilePath, e);
        }
    }

    //Add items
    public synchronized Item addItem(
            String name,
            String description,
            String flavor,
            String size,
            double price,
            int shape,
            String category,
            String availability,
            MultipartFile imageFile) throws IOException {
        validateItem(name, description, flavor, size, price, shape, category, availability);
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("Choose an image for the item.");
        }

        String imageUrl = storeImage(imageFile);
        try {
            Item newItem = new Item(UUID.randomUUID().toString(), name.trim(), description.trim(),
                    flavor.trim(), size.trim(), price, shape, category.trim(), availability, imageUrl);
            List<Item> items = new java.util.ArrayList<>(getAllItems());
            items.add(newItem);
            saveItemsToFile(items);
            return newItem;
        } catch (RuntimeException saveFailure) {
            cleanupImageAfterFailedSave(imageUrl, saveFailure);
            throw saveFailure;
        }
    }

    //update items
    public synchronized Item updateItem(
            String itemId,
            String name,
            String description,
            String flavor,
            String size,
            double price,
            int shape,
            String category,
            String availability,
            MultipartFile imageFile) throws IOException {
        validateItem(name, description, flavor, size, price, shape, category, availability);
        List<Item> items = new java.util.ArrayList<>(getAllItems());
        int itemIndex = findItemIndex(items, itemId);
        Item current = items.get(itemIndex);

        String imageUrl = current.getImageUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = storeImage(imageFile);
        }

        Item updated = new Item(itemId, name.trim(), description.trim(), flavor.trim(), size.trim(),
                price, shape, category.trim(), availability, imageUrl);
        items.set(itemIndex, updated);
        try {
            saveItemsToFile(items);
        } catch (RuntimeException saveFailure) {
            if (!Objects.equals(imageUrl, current.getImageUrl())) {
                cleanupImageAfterFailedSave(imageUrl, saveFailure);
            }
            throw saveFailure;
        }
        if (!Objects.equals(imageUrl, current.getImageUrl())) {
            deleteManagedImage(current.getImageUrl());
        }
        return updated;
    }

    //Delete items
    public synchronized void deleteItemById(String itemId) {
        List<Item> items = new java.util.ArrayList<>(getAllItems());
        int itemIndex = findItemIndex(items, itemId);
        Item deleted = items.remove(itemIndex);
        saveItemsToFile(items);
        deleteManagedImage(deleted.getImageUrl());
    }

    //Get image path
    public Path getImagePath(String filename) throws IOException {
        if (filename == null || !filename.matches("[0-9a-fA-F-]+\\.(png|jpg|webp)")) {
            throw new java.nio.file.NoSuchFileException(filename);
        }
        Path imagePath = imageStoragePath.resolve(filename).normalize();
        if (!imagePath.startsWith(imageStoragePath) || !Files.isRegularFile(imagePath)) {
            throw new java.nio.file.NoSuchFileException(filename);
        }
        return imagePath;
    }

    //Store image
    private String storeImage(MultipartFile imageFile) throws IOException {
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Item images must be 5 MB or smaller.");
        }

        byte[] bytes = imageFile.getBytes();
        String extension = detectImageExtension(bytes);
        if (extension == null) {
            throw new IllegalArgumentException("Upload a valid PNG, JPEG, or WebP image.");
        }

        String filename = UUID.randomUUID() + extension;
        Files.write(imageStoragePath.resolve(filename), bytes, StandardOpenOption.CREATE_NEW);
        return "/item-images/" + filename;
    }

    //Detect image extension
    private String detectImageExtension(byte[] bytes) {
        if (bytes.length >= 8
                && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && bytes[4] == 0x0D && bytes[5] == 0x0A && bytes[6] == 0x1A && bytes[7] == 0x0A) {
            return ".png";
        }
        if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8
                && bytes[2] == (byte) 0xFF) {
            return ".jpg";
        }
        if (bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return ".webp";
        }
        return null;
    }

    //Validate item
    private void validateItem(String name, String description, String flavor, String size,double price, int shape, String category, String availability) {
        if (isBlank(name) || isBlank(description) || isBlank(flavor) || isBlank(size)
                || isBlank(category) || isBlank(availability)) {
            throw new IllegalArgumentException("Complete all required item fields.");
        }
        if (!Double.isFinite(price) || price < 0) {
            throw new IllegalArgumentException("Price must be a valid non-negative amount.");
        }
        if (shape < 0 || shape > 3) {
            throw new IllegalArgumentException("Choose a valid item shape.");
        }
        if (!availability.equals("Available") && !availability.equals("Unavailable")) {
            throw new IllegalArgumentException("Choose a valid availability.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    //find items
    private int findItemIndex(List<Item> items, String itemId) {
        for (int i = 0; i < items.size(); i++) {
            if (Objects.equals(items.get(i).getId(), itemId)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Item not found.");
    }

    private void saveItemsToFile(List<Item> items) {
        Path temporaryFile = null;
        try {
            temporaryFile = Files.createTempFile(databaseFilePath.getParent(), "items-", ".tmp");
            byte[] json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(items);
            Files.write(temporaryFile, json, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temporaryFile, databaseFilePath,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, databaseFilePath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save items to " + databaseFilePath, e);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to remove temporary item data file.", e);
                }
            }
        }
    }

    //Delete image
    private void deleteManagedImage(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith("/item-images/")) {
            return;
        }
        String filename = imageUrl.substring("/item-images/".length());
        try {
            Files.deleteIfExists(getImagePath(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete the item image.", e);
        }
    }

    private void cleanupImageAfterFailedSave(String imageUrl, RuntimeException saveFailure) {
        try {
            deleteManagedImage(imageUrl);
        } catch (RuntimeException cleanupFailure) {
            saveFailure.addSuppressed(cleanupFailure);
        }
    }
}
