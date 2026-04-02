package com.foodrescue.service;

import com.foodrescue.entity.FoodItem;
import com.foodrescue.repository.FoodItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class FoodItemExpirationService {

    @Autowired
    private FoodItemRepository foodItemRepository;

    // Run every hour to check for expired items based on expiryDate
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void expireOldFoodItems() {
        LocalDate today = LocalDate.now();
        List<FoodItem> itemsToExpire = foodItemRepository.findExpiredItems(today);

        for (FoodItem item : itemsToExpire) {
            item.setStatus(FoodItem.Status.EXPIRED);
            foodItemRepository.save(item);
        }

        if (!itemsToExpire.isEmpty()) {
            System.out.println("Auto-expired " + itemsToExpire.size() + " food item(s) past expiry date");
        }
    }
}
