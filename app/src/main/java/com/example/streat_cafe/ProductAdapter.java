package com.example.streat_cafe;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final OnProductClickListener listener;

    public interface OnProductClickListener {
        void onAddToCartClick(Product product);
    }

    public ProductAdapter(Context context, List<Product> productList, OnProductClickListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.tvName.setText(product.getProductName());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "₱ %.2f", product.getPrice()));
        holder.tvCategory.setText(product.getCategory());

        String imageSource = product.getImage();
        if (!TextUtils.isEmpty(imageSource)) {
            if (imageSource.startsWith("http")) {
                Glide.with(context)
                        .load(imageSource)
                        .placeholder(R.drawable.coffee_removebg_preview)
                        .error(R.drawable.coffee_removebg_preview)
                        .into(holder.ivImage);
            } else {
                // Look up as drawable resource
                int foundRes = context.getResources().getIdentifier(imageSource, "drawable", context.getPackageName());
                if (foundRes != 0) {
                    holder.ivImage.setImageResource(foundRes);
                } else {
                    holder.ivImage.setImageResource(R.drawable.coffee_removebg_preview);
                }
            }
        } else {
            holder.ivImage.setImageResource(R.drawable.coffee_removebg_preview);
        }

        if (product.isAvailable()) {
            holder.btnAdd.setEnabled(true);
            holder.btnAdd.setAlpha(1.0f);
            holder.btnAdd.setText("Add to Cart");
            holder.btnAdd.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAddToCartClick(product);
                }
            });
        } else {
            holder.btnAdd.setEnabled(false);
            holder.btnAdd.setAlpha(0.5f);
            holder.btnAdd.setText("Out of Stock");
            holder.btnAdd.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPrice, tvCategory;
        ImageView ivImage;
        Button btnAdd;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            ivImage = itemView.findViewById(R.id.ivProductImage);
            btnAdd = itemView.findViewById(R.id.btnAddToCart);
            
            btnAdd.setFocusable(false);
            btnAdd.setFocusableInTouchMode(false);
        }
    }
}
