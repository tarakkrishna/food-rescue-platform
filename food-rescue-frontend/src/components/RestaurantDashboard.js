import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { restaurantService, userService } from '../services/api';
import LocationPicker from './LocationPicker';

const RestaurantDashboard = () => {
  const { user, logout, updateUser } = useAuth();
  const [foodItems, setFoodItems] = useState([]);
  const [pendingClaims, setPendingClaims] = useState([]);
  const [activeTab, setActiveTab] = useState('food');
  const [formData, setFormData] = useState({
    name: '',
    quantity: '',
    expiryDate: '',
    latitude: null,
    longitude: null
  });
  const [showLocationModal, setShowLocationModal] = useState(false);
  const [message, setMessage] = useState({ type: '', text: '' });
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(null);
  const [rejectReason, setRejectReason] = useState('');
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [selectedClaim, setSelectedClaim] = useState(null);

  const fetchFoodItems = useCallback(async () => {
    try {
      const response = await restaurantService.getFoodList();
      setFoodItems(response.data);
    } catch (err) {
      showMessage('error', 'Failed to load food items');
    }
  }, []);

  const fetchPendingClaims = useCallback(async () => {
    try {
      const response = await restaurantService.getPendingClaims();
      setPendingClaims(response.data);
    } catch (err) {
      showMessage('error', 'Failed to load pending claims');
    }
  }, []);

  useEffect(() => {
    fetchFoodItems();
    fetchPendingClaims();
  }, [fetchFoodItems, fetchPendingClaims]);

  const showMessage = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage({ type: '', text: '' }), 5000);
  };

  const handleLocationSelect = (location) => {
    setFormData({
      ...formData,
      latitude: location.latitude,
      longitude: location.longitude
    });
  };

  const handleUpdateUserLocation = async (location) => {
    try {
      const response = await userService.updateLocation(location.latitude, location.longitude);
      // Update user in AuthContext with new location data
      const updatedUser = { ...user, latitude: location.latitude, longitude: location.longitude };
      updateUser(updatedUser);
      showMessage('success', 'Your location has been updated successfully!');
      setShowLocationModal(false);
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to update location');
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    if (!formData.latitude || !formData.longitude) {
      showMessage('error', 'Please select a pickup location on the map');
      setLoading(false);
      return;
    }
    
    try {
      await restaurantService.addFood(formData);
      showMessage('success', 'Food item added successfully!');
      setFormData({ name: '', quantity: '', expiryDate: '', latitude: null, longitude: null });
      fetchFoodItems();
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to add food item');
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (claimId) => {
    setActionLoading(claimId);
    try {
      await restaurantService.approveClaim(claimId);
      showMessage('success', 'Claim approved successfully!');
      fetchPendingClaims();
      fetchFoodItems();
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to approve claim');
    } finally {
      setActionLoading(null);
    }
  };

  const openRejectModal = (claim) => {
    setSelectedClaim(claim);
    setRejectReason('');
    setShowRejectModal(true);
  };

  const handleReject = async () => {
    if (!selectedClaim) return;
    
    setActionLoading(selectedClaim.id);
    setShowRejectModal(false);
    
    try {
      await restaurantService.rejectClaim(selectedClaim.id, rejectReason);
      showMessage('success', 'Claim rejected successfully');
      fetchPendingClaims();
      fetchFoodItems();
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to reject claim');
    } finally {
      setActionLoading(null);
      setSelectedClaim(null);
      setRejectReason('');
    }
  };

  const getFoodStatusClass = (status) => {
    return status === 'AVAILABLE' ? 'status-available' : 'status-claimed';
  };

  const formatDateTime = (dateString) => {
    return new Date(dateString).toLocaleString();
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>Restaurant Dashboard</h1>
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
              <p className="warning">⚠ Location not set. Please update your location.</p>
            )}
            <button 
              className="btn btn-secondary btn-sm update-location-btn"
              onClick={() => setShowLocationModal(true)}
            >
              📍 Update My Location
            </button>
          </div>
        </div>

        {activeTab === 'food' && (
          <>
            <div className="card">
              <h2>Add New Food Item</h2>
              <form onSubmit={handleSubmit} className="add-food-form">
                <div className="form-row">
                  <div className="form-group">
                    <label>Food Name</label>
                    <input
                      type="text"
                      name="name"
                      value={formData.name}
                      onChange={handleChange}
                      required
                      placeholder="e.g., Fresh Bread"
                    />
                  </div>
                  <div className="form-group">
                    <label>Quantity</label>
                    <input
                      type="text"
                      name="quantity"
                      value={formData.quantity}
                      onChange={handleChange}
                      required
                      placeholder="e.g., 10 loaves"
                    />
                  </div>
                  <div className="form-group">
                    <label>Expiry Date</label>
                    <input
                      type="date"
                      name="expiryDate"
                      value={formData.expiryDate}
                      onChange={handleChange}
                      required
                    />
                  </div>
                </div>

                <div className="form-group">
                  <label>Pickup Location *</label>
                  {formData.latitude && formData.longitude && (
                    <p className="location-selected">
                      ✓ Selected: {formData.latitude.toFixed(6)}, {formData.longitude.toFixed(6)}
                    </p>
                  )}
                  <LocationPicker 
                    onLocationSelect={handleLocationSelect}
                    initialPosition={formData.latitude && formData.longitude ? 
                      { lat: formData.latitude, lng: formData.longitude } : null}
                    height="250px"
                  />
                </div>

                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? 'Adding...' : 'Add Food Item'}
                </button>
              </form>
            </div>

            <div className="card">
              <div className="section-header">
                <h2>Your Food Donations</h2>
                <button 
                  className="btn btn-secondary"
                  onClick={() => setActiveTab('claims')}
                >
                  View Pending Claims ({pendingClaims.length})
                </button>
              </div>
              {foodItems.length === 0 ? (
                <p className="empty-message">No food items added yet.</p>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Food Item</th>
                        <th>Quantity</th>
                        <th>Expiry Date</th>
                        <th>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {foodItems.map((item) => (
                        <tr key={item.id}>
                          <td>{item.name}</td>
                          <td>{item.quantity}</td>
                          <td>{item.expiryDate}</td>
                          <td>
                            <span className={`status-badge ${getFoodStatusClass(item.status)}`}>
                              {item.status}
                            </span>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </>
        )}

        {activeTab === 'claims' && (
          <div className="card">
            <div className="section-header">
              <h2>Pending Claims</h2>
              <button 
                className="btn btn-secondary"
                onClick={() => setActiveTab('food')}
              >
                Back to Food List
              </button>
            </div>
            {pendingClaims.length === 0 ? (
              <p className="empty-message">No pending claims at the moment.</p>
            ) : (
              <div className="claims-list">
                {pendingClaims.map((claim) => (
                  <div key={claim.id} className="claim-card">
                    <div className="claim-info">
                      <h4>{claim.foodItemName}</h4>
                      <p><strong>Quantity:</strong> {claim.quantity}</p>
                      <p><strong>Claimed by:</strong> {claim.ngoName}</p>
                      <p><strong>Claimed on:</strong> {formatDateTime(claim.claimDate)}</p>
                      <span className="status-badge status-pending">PENDING</span>
                    </div>
                    <div className="claim-actions">
                      <button
                        onClick={() => handleApprove(claim.id)}
                        className="btn btn-approve"
                        disabled={actionLoading === claim.id}
                      >
                        {actionLoading === claim.id ? 'Processing...' : 'Approve'}
                      </button>
                      <button
                        onClick={() => openRejectModal(claim)}
                        className="btn btn-reject"
                        disabled={actionLoading === claim.id}
                      >
                        Reject
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {showRejectModal && (
          <div className="modal-overlay">
            <div className="modal">
              <h3>Reject Claim</h3>
              <p>Are you sure you want to reject this claim?</p>
              <div className="form-group">
                <label>Reason (optional):</label>
                <textarea
                  value={rejectReason}
                  onChange={(e) => setRejectReason(e.target.value)}
                  placeholder="Enter reason for rejection..."
                  rows="3"
                />
              </div>
              <div className="modal-actions">
                <button 
                  className="btn btn-secondary"
                  onClick={() => setShowRejectModal(false)}
                >
                  Cancel
                </button>
                <button 
                  className="btn btn-reject"
                  onClick={handleReject}
                  disabled={actionLoading}
                >
                  Reject Claim
                </button>
              </div>
            </div>
          </div>
        )}

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
    </div>
  );
};

export default RestaurantDashboard;
