package com.foodrescue.repository;

import com.foodrescue.entity.FoodItem;
import com.foodrescue.entity.User;
import com.foodrescue.entity.VolunteerRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerRequestRepository extends JpaRepository<VolunteerRequest, Long> {

    List<VolunteerRequest> findByDonation(FoodItem donation);

    List<VolunteerRequest> findByVolunteer(User volunteer);

    Optional<VolunteerRequest> findByDonationAndVolunteer(FoodItem donation, User volunteer);

    boolean existsByDonationAndVolunteer(FoodItem donation, User volunteer);

    List<VolunteerRequest> findByDonationAndStatus(FoodItem donation, VolunteerRequest.Status status);
}
