package com.foodrescue.repository;

import com.foodrescue.entity.FoodItem;
import com.foodrescue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findByRestaurant(User restaurant);
    List<FoodItem> findByStatus(FoodItem.Status status);
    List<FoodItem> findByRestaurantId(Long restaurantId);
    long countByStatus(FoodItem.Status status);
    
    @Query("SELECT f FROM FoodItem f WHERE f.status = 'POSTED' AND f.expiryDate < :today")
    List<FoodItem> findExpiredItems(@Param("today") LocalDate today);
    
    List<FoodItem> findByVolunteer(User volunteer);
    
    @Query("SELECT f FROM FoodItem f WHERE f.volunteer.id = :volunteerId")
    List<FoodItem> findByVolunteerId(@Param("volunteerId") Long volunteerId);
    
    @Query("SELECT f FROM FoodItem f WHERE f.status IN ('POSTED', 'CLAIMED') AND f.volunteer IS NULL")
    List<FoodItem> findUnassignedDonations();

    @Query("SELECT f FROM FoodItem f WHERE f.status IN ('POSTED', 'CLAIMED') AND f.volunteer IS NULL AND f.latitude IS NOT NULL AND f.longitude IS NOT NULL")
    List<FoodItem> findUnassignedDonationsWithLocation();

    @Query("SELECT f.restaurant.id, f.restaurant.name, COUNT(f) as cnt FROM FoodItem f GROUP BY f.restaurant.id, f.restaurant.name ORDER BY cnt DESC")
    List<Object[]> findTopRestaurantsByFoodAdded();
}
