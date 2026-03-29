package com.example.streat_cafe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private String id;
    private String productName;
    private String productDescription;
    private double price;
    private String category;
    private String image;
    private boolean available;
    private List<Size> sizes;
    private List<Addon> addons;
    private List<SugarLevel> sugarLevels;

    public static class Size {
        public String id;
        public String name;
        public double price;
        public Size(String id, String name, double price) { this.id = id; this.name = name; this.price = price; }
    }

    public static class Addon {
        public String id;
        public String name;
        public double price;
        public Addon(String id, String name, double price) { this.id = id; this.name = name; this.price = price; }
    }

    public static class SugarLevel {
        public String id;
        public String name;
        public int value;
        public SugarLevel(String id, String name, int value) { this.id = id; this.name = name; this.value = value; }
    }

    public Product(String id, String productName, String productDescription, double price, String category, String image, boolean available, List<Size> sizes, List<Addon> addons, List<SugarLevel> sugarLevels) {
        this.id = id;
        this.productName = productName;
        this.productDescription = productDescription;
        this.price = price;
        this.category = category;
        this.image = image;
        this.available = available;
        this.sizes = sizes;
        this.addons = addons;
        this.sugarLevels = sugarLevels;
    }

    public static Product fromJson(JsonObject p) {
        String id = p.has("_id") ? p.get("_id").getAsString() : "";
        String name = p.has("productName") ? p.get("productName").getAsString() : "Unknown";
        String desc = p.has("productDescription") ? p.get("productDescription").getAsString() : "";
        double price = p.has("price") ? p.get("price").getAsDouble() : 0.0;
        String category = p.has("category") ? p.get("category").getAsString() : "General";
        String imageSource = p.has("image") ? p.get("image").getAsString() : "";
        boolean avail = !p.has("available") || p.get("available").getAsBoolean();

        List<Size> sizesList = new ArrayList<>();
        if (p.has("sizes") && p.get("sizes").isJsonArray()) {
            for (JsonElement e : p.getAsJsonArray("sizes")) {
                JsonObject o = e.getAsJsonObject();
                sizesList.add(new Size(o.has("_id") ? o.get("_id").getAsString() : "", o.get("name").getAsString(), o.get("price").getAsDouble()));
            }
        }

        List<Addon> addonsList = new ArrayList<>();
        if (p.has("addons") && p.get("addons").isJsonArray()) {
            for (JsonElement e : p.getAsJsonArray("addons")) {
                JsonObject o = e.getAsJsonObject();
                addonsList.add(new Addon(o.has("_id") ? o.get("_id").getAsString() : "", o.get("name").getAsString(), o.get("price").getAsDouble()));
            }
        }

        List<SugarLevel> sugarList = new ArrayList<>();
        if (p.has("sugarLevel") && p.get("sugarLevel").isJsonArray()) {
            for (JsonElement e : p.getAsJsonArray("sugarLevel")) {
                JsonObject o = e.getAsJsonObject();
                sugarList.add(new SugarLevel(o.has("_id") ? o.get("_id").getAsString() : "", o.get("name").getAsString(), o.get("value").getAsInt()));
            }
        }

        return new Product(id, name, desc, price, category, imageSource, avail, sizesList, addonsList, sugarList);
    }

    public String getId() { return id; }
    public String getProductName() { return productName; }
    public String getProductDescription() { return productDescription; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public String getImage() { return image; }
    public boolean isAvailable() { return available; }
    public List<Size> getSizes() { return sizes; }
    public List<Addon> getAddons() { return addons; }
    public List<SugarLevel> getSugarLevels() { return sugarLevels; }
}
