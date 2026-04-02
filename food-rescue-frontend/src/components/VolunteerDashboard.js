import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { volunteerService, userService } from '../services/api';
import LocationPicker from './LocationPicker';

const VolunteerDashboard = () => {
  const { user, logout, updateUser } = useAuth();
  const [assignments, setAssignments] = useState([]);
  const [nearbyPickups, setNearbyPickups] = useState([]);
  const [nearbyDrops, setNearbyDrops] = useState([]);
  const [activeTab, setActiveTab] = useState('nearby');
  const [message, setMessage] = useState({ type: '', text: '' });
  const [actionLoading, setActionLoading] = useState(null);
  const [showLocationModal, setShowLocationModal] = useState(false);

  const showMessage = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage({ type: '', text: '' }), 5000);
  };

  const fetchAssignments = useCallback(async () => {
    try {
      const response = await volunteerService.getMyAssignments();
      setAssignments(response.data || []);
    } catch (err) {
      showMessage('error', 'Failed to load assignments');
    }
  }, []);

  const fetchNearbyTasks = useCallback(async () => {
    if (!user?.latitude || !user?.longitude) {
      showMessage('error', 'Please set your location to see nearby tasks');
      return;
    }
    
    try {
      // Fetch nearby pickups (claimed food ready for pickup)
      const pickupsResponse = await fetch(`${process.env.REACT_APP_API_URL || 'http://localhost:8080'}/volunteer/nearby-pickups?lat=${user.latitude}&lon=${user.longitude}&radiusKm=10`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      // Fetch nearby drops (food ready for delivery)
      const dropsResponse = await fetch(`${process.env.REACT_APP_API_URL || 'http://localhost:8080'}/volunteer/nearby-drops?lat=${user.latitude}&lon=${user.longitude}&radiusKm=10`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (pickupsResponse.ok) {
        const pickupsData = await pickupsResponse.json();
        setNearbyPickups(pickupsData || []);
      }
      
      if (dropsResponse.ok) {
        const dropsData = await dropsResponse.json();
        setNearbyDrops(dropsData || []);
      }
    } catch (err) {
      showMessage('error', 'Failed to load nearby tasks');
    }
  }, [user]);

  useEffect(() => {
    fetchAssignments();
    fetchNearbyTasks();
  }, [fetchAssignments, fetchNearbyTasks]);

  const handlePickup = async (donationId) => {
    setActionLoading(donationId);
    try {
      await volunteerService.pickupDonation(donationId);
      showMessage('success', 'Donation picked up successfully!');
      fetchAssignments();
      fetchNearbyTasks();
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to pickup donation');
    } finally {
      setActionLoading(null);
    }
  };

  const handleDeliver = async (donationId) => {
    setActionLoading(donationId);
    try {
      await volunteerService.deliverDonation(donationId);
      showMessage('success', 'Donation delivered successfully!');
      fetchAssignments();
      fetchNearbyTasks();
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to deliver donation');
    } finally {
      setActionLoading(null);
    }
  };

  const handleGetRoute = async (pickupLat, pickupLng, dropLat, dropLng) => {
    if (!pickupLat || !pickupLng || !dropLat || !dropLng) {
      showMessage('error', 'Location coordinates not available');
      return;
    }
    
    const origin = `${pickupLat},${pickupLng}`;
    const destination = `${dropLat},${dropLng}`;
    const googleMapsUrl = `https://www.google.com/maps/dir/?api=1&origin=${origin}&destination=${destination}&travelmode=driving`;
    
    window.open(googleMapsUrl, '_blank');
  };

  const acceptPickupTask = async (claimId) => {
    setActionLoading(claimId);
    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL || 'http://localhost:8080'}/volunteer/accept-pickup/${claimId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (response.ok) {
        showMessage('success', 'Pickup task accepted!');
        fetchNearbyTasks();
        fetchAssignments();
      } else {
        const error = await response.json();
        showMessage('error', error.message || 'Failed to accept task');
      }
    } catch (err) {
      showMessage('error', 'Failed to accept pickup task');
    } finally {
      setActionLoading(null);
    }
  };

  const acceptDeliveryTask = async (claimId) => {
    setActionLoading(claimId);
    try {
      const response = await fetch(`${process.env.REACT_APP_API_URL || 'http://localhost:8080'}/volunteer/accept-delivery/${claimId}`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (response.ok) {
        showMessage('success', 'Delivery task accepted!');
        fetchNearbyTasks();
        fetchAssignments();
      } else {
        const error = await response.json();
        showMessage('error', error.message || 'Failed to accept task');
      }
    } catch (err) {
      showMessage('error', 'Failed to accept delivery task');
    } finally {
      setActionLoading(null);
    }
  };

  const handleUpdateUserLocation = async (location) => {
    try {
      const response = await userService.updateLocation(location.latitude, location.longitude);
      // Update user in AuthContext with new location data from response
      const updatedUser = { ...user, latitude: location.latitude, longitude: location.longitude };
      updateUser(updatedUser);
      showMessage('success', 'Your location has been updated successfully!');
      setShowLocationModal(false);
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to update location');
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'ASSIGNED': return 'status-pending';
      case 'PICKED_UP': return 'status-approved';
      case 'DELIVERED': return 'status-delivered';
      default: return '';
    }
  };

  const filteredAssignments = assignments.filter(item => {
    if (activeTab === 'assigned') return item.status === 'ASSIGNED';
    if (activeTab === 'in-progress') return item.status === 'PICKED_UP';
    if (activeTab === 'completed') return item.status === 'DELIVERED';
    return true;
  });

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>Volunteer Dashboard</h1>
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
              <p className="warning">⚠ Location not set. Please update your location to find nearby tasks.</p>
            )}
            <button 
              className="btn btn-secondary btn-sm update-location-btn"
              onClick={() => setShowLocationModal(true)}
            >
              📍 Update My Location
            </button>
          </div>
        </div>

        <div className="tabs">
          <button
            className={`tab-btn ${activeTab === 'nearby' ? 'active' : ''}`}
            onClick={() => setActiveTab('nearby')}
          >
            Nearby Tasks ({nearbyPickups.length + nearbyDrops.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'assigned' ? 'active' : ''}`}
            onClick={() => setActiveTab('assigned')}
          >
            Assigned ({assignments.filter(i => i.status === 'ASSIGNED').length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'in-progress' ? 'active' : ''}`}
            onClick={() => setActiveTab('in-progress')}
          >
            In Progress ({assignments.filter(i => i.status === 'PICKED_UP').length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'completed' ? 'active' : ''}`}
            onClick={() => setActiveTab('completed')}
          >
            Completed ({assignments.filter(i => i.status === 'DELIVERED').length})
          </button>
        </div>

        {activeTab === 'nearby' && (
          <div>
            <div className="card">
              <h2>🚐 Nearby Pickups (Ready for Collection)</h2>
              {nearbyPickups.length === 0 ? (
                <p className="empty-message">No nearby pickups available. Check back later!</p>
              ) : (
                <div className="tasks-grid">
                  {nearbyPickups.map((task) => (
                    <div key={task.id} className="task-card">
                      <div className="task-header">
                        <h4>{task.foodItemName}</h4>
                        <span className="status-badge status-pending">Ready for Pickup</span>
                      </div>
                      <div className="task-details">
                        <p><strong>Restaurant:</strong> {task.restaurantName}</p>
                        <p><strong>Quantity:</strong> {task.quantity}</p>
                        <p><strong>NGO:</strong> {task.ngoName}</p>
                        <p><strong>Distance:</strong> {task.distanceKm?.toFixed(1)} km</p>
                      </div>
                      <div className="task-actions">
                        <button
                          onClick={() => handleGetRoute(
                            user.latitude, user.longitude,
                            task.restaurantLatitude, task.restaurantLongitude
                          )}
                          className="btn btn-secondary btn-sm"
                        >
                          🗺️ Route to Restaurant
                        </button>
                        <button
                          onClick={() => acceptPickupTask(task.id)}
                          className="btn btn-primary btn-sm"
                          disabled={actionLoading === task.id}
                        >
                          {actionLoading === task.id ? 'Accepting...' : 'Accept Pickup'}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="card">
              <h2>🏠 Nearby Drops (Ready for Delivery)</h2>
              {nearbyDrops.length === 0 ? (
                <p className="empty-message">No nearby deliveries available. Check back later!</p>
              ) : (
                <div className="tasks-grid">
                  {nearbyDrops.map((task) => (
                    <div key={task.id} className="task-card">
                      <div className="task-header">
                        <h4>{task.foodItemName}</h4>
                        <span className="status-badge status-approved">Ready for Delivery</span>
                      </div>
                      <div className="task-details">
                        <p><strong>From:</strong> {task.restaurantName}</p>
                        <p><strong>To:</strong> {task.ngoName}</p>
                        <p><strong>Quantity:</strong> {task.quantity}</p>
                        <p><strong>Distance:</strong> {task.distanceKm?.toFixed(1)} km</p>
                      </div>
                      <div className="task-actions">
                        <button
                          onClick={() => handleGetRoute(
                            task.pickupLatitude, task.pickupLongitude,
                            task.dropLatitude, task.dropLongitude
                          )}
                          className="btn btn-secondary btn-sm"
                        >
                          🗺️ Show Route
                        </button>
                        <button
                          onClick={() => acceptDeliveryTask(task.id)}
                          className="btn btn-success btn-sm"
                          disabled={actionLoading === task.id}
                        >
                          {actionLoading === task.id ? 'Accepting...' : 'Accept Delivery'}
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        )}

        <div className="card">
          <h2>
            {activeTab === 'assigned' && 'New Assignments'}
            {activeTab === 'in-progress' && 'In Progress Deliveries'}
            {activeTab === 'completed' && 'Completed Deliveries'}
          </h2>
          
          {filteredAssignments.length === 0 ? (
            <p className="empty-message">
              {activeTab === 'assigned' && 'No new assignments. NGOs will assign you when food is claimed.'}
              {activeTab === 'in-progress' && 'No deliveries in progress.'}
              {activeTab === 'completed' && 'No completed deliveries yet.'}
            </p>
          ) : (
            <div className="assignments-list">
              {filteredAssignments.map((item) => (
                <div key={item.id} className="assignment-card">
                  <div className="assignment-header">
                    <h3>{item.name}</h3>
                    <span className={`status-badge ${getStatusBadgeClass(item.status)}`}>
                      {item.status}
                    </span>
                  </div>
                  <div className="assignment-details">
                    <p><strong>Quantity:</strong> {item.quantity}</p>
                    <p><strong>Restaurant:</strong> {item.restaurantName || 'Unknown'}</p>
                    <p><strong>Expiry Date:</strong> {item.expiryDate}</p>
                  </div>
                  <div className="assignment-actions">
                    {item.status === 'ASSIGNED' && (
                      <button
                        onClick={() => handlePickup(item.id)}
                        className="btn btn-primary"
                        disabled={actionLoading === item.id}
                      >
                        {actionLoading === item.id ? 'Processing...' : 'Mark as Picked Up'}
                      </button>
                    )}
                    {item.status === 'PICKED_UP' && (
                      <button
                        onClick={() => handleDeliver(item.id)}
                        className="btn btn-success"
                        disabled={actionLoading === item.id}
                      >
                        {actionLoading === item.id ? 'Processing...' : 'Mark as Delivered'}
                      </button>
                    )}
                    {item.status === 'DELIVERED' && (
                      <span className="completed-text">✓ Delivered</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
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

export default VolunteerDashboard;
