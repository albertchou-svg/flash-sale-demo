package com.example.flashsale.repository;

import com.example.flashsale.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

// 繼承 JpaRepository 後，自動擁有 save, findById, findAll, delete 等功能
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 【新增】使用 SQL 直接更新庫存 (效能比查出來再 set 好)
    @Modifying
    @Query("UPDATE Product p SET p.stock = p.stock - 1 WHERE p.id = :id AND p.stock > 0")
    int decreaseStock(@Param("id") Long id);
}