package com.foodrescue.repository;

import com.foodrescue.entity.Claim;
import com.foodrescue.entity.FoodItem;
import com.foodrescue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    List<Claim> findByNgo(User ngo);
    List<Claim> findByFoodItem(FoodItem foodItem);
    Optional<Claim> findByFoodItemAndNgo(FoodItem foodItem, User ngo);
    List<Claim> findByNgoId(Long ngoId);
    
    List<Claim> findByFoodItemRestaurantAndStatus(User restaurant, Claim.Status status);
    List<Claim> findByFoodItemRestaurant(User restaurant);
    List<Claim> findByStatus(Claim.Status status);
    long countByStatus(Claim.Status status);
    
    @Query("SELECT c.ngo.id, c.ngo.name, COUNT(c) as cnt FROM Claim c WHERE c.status = 'APPROVED' GROUP BY c.ngo.id, c.ngo.name ORDER BY cnt DESC")
    List<Object[]> findTopNgosByClaims();
}
