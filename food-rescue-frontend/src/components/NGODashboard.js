import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { ngoService, userService } from '../services/api';
import LocationPicker from './LocationPicker';

const NGODashboard = () => {
  const { user, logout, updateUser } = useAuth();
  const [availableFood, setAvailableFood] = useState([]);
  const [myClaims, setMyClaims] = useState([]);
  const [claimedIds, setClaimedIds] = useState([]); // Simple array to track claimed food IDs
  const [activeTab, setActiveTab] = useState('available');
  const [message, setMessage] = useState({ type: '', text: '' });
  const [claimingId, setClaimingId] = useState(null);
  const [pickingUpId, setPickingUpId] = useState(null);
  const [showLocationModal, setShowLocationModal] = useState(false);
  const [nearbyFood, setNearbyFood] = useState([]);
  const [showNearby, setShowNearby] = useState(false);

  const showMessage = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage({ type: '', text: '' }), 5000);
  };

  const fetchAvailableFood = useCallback(async () => {
    try {
      const response = await ngoService.getAvailableFood();
      setAvailableFood(response.data);
    } catch (err) {
      showMessage('error', 'Failed to load available food');
    }
  }, []);

  const fetchMyClaims = useCallback(async () => {
    try {
      const response = await ngoService.getMyClaims();
      setMyClaims(response.data);
      // Extract food IDs from claims
      const ids = response.data.map(claim => claim.foodItemId || claim.foodItem?.id).filter(Boolean);
      setClaimedIds(ids);
    } catch (err) {
      console.error('Fetch claims error:', err);
    }
  }, []);

  // Simple check if food is claimed
  const isClaimed = (foodId) => claimedIds.includes(foodId);

  useEffect(() => {
    fetchAvailableFood();
    fetchMyClaims();
  }, []);

  const handleUpdateUserLocation = async (location) => {
    console.log('=== UPDATE LOCATION ===');
    console.log('Location:', location);
    
    const token = localStorage.getItem('token');
    if (!token) {
      showMessage('error', 'Please login to update location');
      return;
    }
    
    try {
      const response = await fetch('http://localhost:8080/api/users/update-location', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        credentials: 'include',
        body: JSON.stringify({
          latitude: location.latitude,
          longitude: location.longitude
        })
      });
      
      console.log('Update location response status:', response.status);
      
      if (!response.ok) {
        // Try to get error as text first, then parse as JSON if possible
        const errorText = await response.text();
        console.error('Error response text:', errorText);
        
        // If API fails, update locally and show warning
        if (response.status === 500) {
          console.log('API failed, updating location locally only');
          const updatedUser = { 
            ...user, 
            latitude: location.latitude, 
            longitude: location.longitude 
          };
          updateUser(updatedUser);
          showMessage('warning', 'Location set locally (server update failed). Reserve Food may not work until server is fixed.');
          setShowLocationModal(false);
          fetchAvailableFood();
          return;
        }
        
        let errorData;
        try {
          errorData = JSON.parse(errorText);
        } catch (e) {
          errorData = { message: `Server error: ${response.status}` };
        }
        
        console.error('Update location error:', errorData);
        throw new Error(errorData.message || `HTTP ${response.status}`);
      }
      
      const data = await response.json();
      console.log('Update location success:', data);
      
      // Update user in AuthContext with new location data from response
      const updatedUser = { 
        ...user, 
        latitude: data.latitude || location.latitude, 
        longitude: data.longitude || location.longitude 
      };
      updateUser(updatedUser);
      showMessage('success', 'Your location has been updated successfully!');
      setShowLocationModal(false);
      
      // Refresh food lists with new location
      fetchAvailableFood();
      
    } catch (error) {
      console.error('Update location error:', error);
      
      // Fallback: update location locally if API fails
      if (error.message.includes('500')) {
        console.log('API failed, updating location locally only');
        const updatedUser = { 
          ...user, 
          latitude: location.latitude, 
          longitude: location.longitude 
        };
        updateUser(updatedUser);
        showMessage('warning', 'Location set locally (server update failed). Reserve Food may not work until server is fixed.');
        setShowLocationModal(false);
        fetchAvailableFood();
      } else {
        showMessage('error', error.message || 'Failed to update location');
      }
    }
  };

  const fetchNearbyFood = useCallback(async () => {
    if (!user?.latitude || !user?.longitude) {
      showMessage('error', 'Please set your location first to see nearby donations');
      return;
    }
    try {
      const response = await ngoService.getNearbyDonations(user.latitude, user.longitude, 10);
      setNearbyFood(response.data);
      setShowNearby(true);
    } catch (err) {
      showMessage('error', 'Failed to load nearby food');
    }
  }, [user]);

  const handleClaim = async (foodId) => {
    console.log('Reserve Food clicked:', foodId);
    
    const token = localStorage.getItem('token');
    if (!token) {
      showMessage('error', 'Please login to reserve food');
      return;
    }
    
    if (!user?.latitude || !user?.longitude) {
      showMessage('error', 'Please set your location first');
      setShowLocationModal(true);
      return;
    }
    
    setClaimingId(foodId);
    
    try {
      const response = await fetch(`http://localhost:8080/food/claim/${foodId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        }
      });
      
      const data = await response.json();
      
      if (!response.ok) {
        // If already claimed, add to our list
        if (data.message?.includes('already claimed')) {
          setClaimedIds(prev => [...prev, foodId]);
        }
        throw new Error(data.message || 'Failed to reserve');
      }
      
      // Success - add to claimed list
      setClaimedIds(prev => [...prev, foodId]);
      showMessage('success', 'Food reserved! Pickup within 2 hours.');
      
      // Refresh lists
      fetchAvailableFood();
      fetchMyClaims();
      
    } catch (error) {
      console.error('Reserve error:', error);
      showMessage('error', error.message);
    } finally {
      setClaimingId(null);
    }
  };

  const handlePickup = async (claimId) => {
    setPickingUpId(claimId);
    try {
      await ngoService.pickupFood(claimId);
      showMessage('success', 'Food picked up successfully! Please deliver to your center.');
      fetchMyClaims();
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to pickup food');
    } finally {
      setPickingUpId(null);
    }
  };

  const requestTransport = async (claimId) => {
    try {
      const response = await fetch(`http://localhost:8080/food/request-transport/${claimId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (response.ok) {
        showMessage('success', 'Transport request sent to nearby volunteers!');
        fetchMyClaims();
      } else {
        const error = await response.json();
        showMessage('error', error.message || 'Failed to request transport');
      }
    } catch (err) {
      showMessage('error', 'Failed to request transport');
    }
  };

  const handleGetRoute = async (foodItemId, item) => {
    console.log('Get route for item:', foodItemId, item);
    
    // Get NGO's location - try browser location first, then fallback to saved location
    let ngoLat, ngoLng;
    
    if (navigator.geolocation) {
      try {
        const position = await new Promise((resolve, reject) => {
          navigator.geolocation.getCurrentPosition(resolve, reject, {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 300000 // 5 minutes
          });
        });
        ngoLat = position.coords.latitude;
        ngoLng = position.coords.longitude;
        console.log('Using browser location:', ngoLat, ngoLng);
      } catch (err) {
        console.log('Browser location not available, using saved location');
        if (!user?.latitude || !user?.longitude) {
          showMessage('error', 'Please enable location access or set your location manually');
          return;
        }
        ngoLat = user.latitude;
        ngoLng = user.longitude;
      }
    } else if (!user?.latitude || !user?.longitude) {
      showMessage('error', 'Please set your location first to get directions');
      return;
    } else {
      ngoLat = user.latitude;
      ngoLng = user.longitude;
    }
    
    let foodLat = item?.latitude;
    let foodLng = item?.longitude;
    
    // If food location is missing, try to get it from the available food list
    if (!foodLat || !foodLng) {
      console.log('Food location missing, searching in available food...');
      const foodItem = availableFood.find(f => f.id === foodItemId);
      if (foodItem?.latitude && foodItem?.longitude) {
        foodLat = foodItem.latitude;
        foodLng = foodItem.longitude;
        console.log('Found food location:', foodLat, foodLng);
      } else {
        showMessage('error', 'Food item location not available');
        return;
      }
    }
    
    const ngoLocation = `${ngoLat},${ngoLng}`;
    const pickupLocation = `${foodLat},${foodLng}`;
    const googleMapsUrl = `https://www.google.com/maps/dir/?api=1&origin=${ngoLocation}&destination=${pickupLocation}&travelmode=driving`;
    
    console.log('NGO Location:', ngoLocation);
    console.log('Pickup Location:', pickupLocation);
    console.log('Opening Google Maps:', googleMapsUrl);
    
    window.open(googleMapsUrl, '_blank');
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString();
  };

  const formatDateTime = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'PENDING': return 'status-pending';
      case 'PICKED_UP': return 'status-approved';
      case 'DELIVERED': return 'status-delivered';
      case 'CANCELLED': return 'status-rejected';
      default: return '';
    }
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>NGO Dashboard</h1>
          <div className="user-info">
            <span>Welcome, <strong>{user?.name}</strong></span>
            <span className="role-badge">{user?.role}</span>
            <button onClick={logout} className="btn btn-logout">Logout</button>
          </div>
        </div>
      </header>

      <div className="dashboard-content">
        {message.text && (
          <div className={`message ${message.type}`}>{message.text}</div>
        )}

        <div className="card location-status-card">
          <div className="location-status">
            <h3>Your Location</h3>
            {user?.latitude && user?.longitude ? (
              <p>✓ Location set: {user.latitude.toFixed(6)}, {user.longitude.toFixed(6)}</p>
            ) : (
              <p className="warning">⚠ Location not set. Please update your location to see nearby donations.</p>
            )}
            <div className="location-actions">
              <button 
                className="btn btn-secondary btn-sm"
                onClick={() => setShowLocationModal(true)}
              >
                📍 Update My Location
              </button>
              <button 
                className="btn btn-success btn-sm"
                onClick={async () => {
                  if (navigator.geolocation) {
                    try {
                      const position = await new Promise((resolve, reject) => {
                        navigator.geolocation.getCurrentPosition(resolve, reject);
                      });
                      const lat = position.coords.latitude;
                      const lng = position.coords.longitude;
                      await handleUpdateUserLocation({ latitude: lat, longitude: lng });
                    } catch (err) {
                      showMessage('error', 'Could not get your location. Please set it manually.');
                    }
                  } else {
                    showMessage('error', 'Geolocation not supported. Please set location manually.');
                  }
                }}
              >
                📍 Use Current Location
              </button>
              <button 
                className="btn btn-primary btn-sm"
                onClick={fetchNearbyFood}
                disabled={!user?.latitude || !user?.longitude}
              >
                🔍 Find Nearby Donations
              </button>
              <button 
                className="btn btn-warning btn-sm"
                onClick={() => {
                  console.log('Manual claims refresh...');
                  fetchMyClaims();
                }}
              >
                🔄 Refresh Claims
              </button>
              <button 
                className="btn btn-info btn-sm"
                onClick={() => {
                  console.log('Testing API connection...');
                  fetch('http://localhost:8080/food/available', {
                    headers: {
                      'Authorization': `Bearer ${localStorage.getItem('token')}`
                    }
                  })
                  .then(r => console.log('API Test Response:', r.status))
                  .catch(e => console.error('API Test Error:', e));
                }}
              >
                🔧 Test API
              </button>
            </div>
          </div>
        </div>

        {showNearby && (
          <div className="card nearby-section">
            <h3>Nearby Donations (within 10km)</h3>
            {nearbyFood.length === 0 ? (
              <p className="empty-message">No donations found nearby.</p>
            ) : (
              <div className="nearby-items-list">
                {nearbyFood.map((item) => {
                  const nearbyClaimed = isClaimed(item.id);
                  return (
                    <div key={item.id} className="nearby-item-card">
                      <h4>{item.name}</h4>
                      <p><strong>Restaurant:</strong> {item.restaurantName}</p>
                      <p><strong>Quantity:</strong> {item.quantity}</p>
                      <p><strong>Expires:</strong> {formatDate(item.expiryDate)}</p>
                      <span className="distance-badge">{item.distanceKm.toFixed(2)} km away</span>
                      {nearbyClaimed && (
                        <span className="status-badge status-pending">Already Claimed</span>
                      )}
                      <button
                        onClick={() => handleClaim(item.id)}
                        className={`btn ${nearbyClaimed ? 'btn-disabled' : 'btn-claim'} btn-sm`}
                        disabled={nearbyClaimed || claimingId === item.id}
                      >
                        {nearbyClaimed ? 'Already Claimed' : 
                         claimingId === item.id ? 'Claiming...' : 'Claim Food'}
                      </button>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        <div className="tabs">
          <button
            className={`tab-btn ${activeTab === 'available' ? 'active' : ''}`}
            onClick={() => setActiveTab('available')}
          >
            Available Food ({availableFood.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'claims' ? 'active' : ''}`}
            onClick={() => setActiveTab('claims')}
          >
            My Claims ({myClaims.length})
          </button>
        </div>

        {activeTab === 'available' && (
          <div className="card">
            <h2>Available Food Donations (within 30km)</h2>
            {availableFood.length === 0 ? (
              <p className="empty-message">No available food donations within 30km of your location. Please update your location or check back later.</p>
            ) : (
              <div className="food-grid">
                {availableFood.map(item => {
                  const claimed = isClaimed(item.id);
                  return (
                    <div key={item.id} className="food-card">
                      <div className="food-card-header">
                        <h4>{item.name}</h4>
                        <span className="restaurant-name">{item.restaurantName}</span>
                        {item.distanceKm && (
                          <span className="distance-badge">{item.distanceKm.toFixed(1)} km away</span>
                        )}
                        {claimed && (
                          <span className="status-badge status-pending">Already Claimed</span>
                        )}
                      </div>
                      <div className="food-card-body">
                        <p><strong>Quantity:</strong> {item.quantity}</p>
                        <p><strong>Expires:</strong> {formatDate(item.expiryDate)}</p>
                      </div>
                      <div className="food-card-footer">
                        <button
                          onClick={() => {
                            console.log('Get Directions clicked for item:', item.id);
                            handleGetRoute(item.id, item);
                          }}
                          className="btn btn-secondary btn-sm"
                          style={{ cursor: 'pointer', zIndex: 10, position: 'relative' }}
                        >
                          🗺️ Get Directions
                        </button>
                        <button
                          onClick={() => {
                            console.log('Reserve Food clicked for item:', item.id);
                            handleClaim(item.id);
                          }}
                          className={`btn ${claimed ? 'btn-disabled' : 'btn-claim'}`}
                          disabled={claimed || claimingId === item.id}
                          style={{ 
                            cursor: claimed ? 'not-allowed' : 'pointer', 
                            zIndex: 10, 
                            position: 'relative',
                            opacity: claimed ? 0.6 : 1
                          }}
                        >
                          {claimed ? 'Already Claimed' : 
                           claimingId === item.id ? 'Reserving...' : 'Reserve Food'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        )}

        {activeTab === 'claims' && (
          <div className="card">
            <h2>Your Food Claims</h2>
            {myClaims.length === 0 ? (
              <p className="empty-message">You haven't claimed any food items yet.</p>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Food Item</th>
                      <th>Restaurant</th>
                      <th>Reserved On</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {myClaims.map((claim) => (
                      <tr key={claim.id}>
                        <td>{claim.foodItemName}</td>
                        <td>{claim.restaurantName || 'Unknown'}</td>
                        <td>{formatDateTime(claim.claimDate)}</td>
                        <td>
                          <span className={`status-badge ${getStatusBadgeClass(claim.status)}`}>
                            {claim.status === 'PENDING' ? 'Pending Pickup' : claim.status.replace('_', ' ')}
                          </span>
                        </td>
                        <td>
                          {claim.status === 'PENDING' && (
                            <div className="claim-actions">
                              <button
                                onClick={() => handleGetRoute(claim.foodItemId, { latitude: claim.foodItemLatitude, longitude: claim.foodItemLongitude })}
                                className="btn btn-secondary btn-sm"
                              >
                                🗺️ Directions
                              </button>
                              <button
                                onClick={() => requestTransport(claim.id)}
                                className="btn btn-warning btn-sm"
                              >
                                🚐 Request Transport
                              </button>
                              <button
                                onClick={() => handlePickup(claim.id)}
                                className="btn btn-primary btn-sm"
                                disabled={pickingUpId === claim.id}
                              >
                                {pickingUpId === claim.id ? 'Confirming...' : '✓ Confirm Pickup'}
                              </button>
                            </div>
                          )}
                          {claim.status === 'PICKED_UP' && (
                            <small>Picked up on {formatDateTime(claim.pickupDate || claim.pickupTime)}</small>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}
      </div>

      {showLocationModal && (
        <div className="location-modal-overlay">
          <div className="location-modal">
            <h3>Update Your Location</h3>
            <p>Select your current location on the map or use your device's location.</p>
            <LocationPicker 
              onLocationSelect={handleUpdateUserLocation}
              initialPosition={user?.latitude && user?.longitude ? 
                { lat: user.latitude, lng: user.longitude } : null}
              height="300px"
            />
            <div className="modal-actions">
              <button 
                className="btn btn-secondary"
                onClick={() => setShowLocationModal(false)}
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default NGODashboard;
