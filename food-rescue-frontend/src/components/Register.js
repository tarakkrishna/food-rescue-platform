import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { authService } from '../services/api';
import LocationPicker from './LocationPicker';

const Register = () => {
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    password: '',
    role: 'RESTAURANT',
    latitude: null,
    longitude: null
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [showMap, setShowMap] = useState(false);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleLocationSelect = (location) => {
    setFormData({
      ...formData,
      latitude: location.latitude,
      longitude: location.longitude
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (!formData.latitude || !formData.longitude) {
      setError('Please select your location on the map');
      return;
    }

    setLoading(true);

    try {
      const response = await authService.register(
        formData.name,
        formData.email,
        formData.password,
        formData.role,
        formData.latitude,
        formData.longitude
      );
      setSuccess(response.data.message || 'Registration successful! Redirecting to login...');
      setTimeout(() => navigate('/login'), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-card">
        <h2>Create Account</h2>
        <p className="auth-subtitle">Join Food Rescue as a Restaurant, NGO, or Volunteer</p>
        
        {error && <div className="error-message">{error}</div>}
        {success && <div className="success-message">{success}</div>}
        
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Organization Name</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              required
              placeholder="Enter organization name"
            />
          </div>
          
          <div className="form-group">
            <label>Email</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              placeholder="Enter email address"
            />
          </div>
          
          <div className="form-group">
            <label>Password</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              minLength="6"
              placeholder="Create a password"
            />
          </div>
          
          <div className="form-group">
            <label>Account Type</label>
            <select name="role" value={formData.role} onChange={handleChange}>
              <option value="RESTAURANT">Restaurant (Donate Food)</option>
              <option value="NGO">NGO (Claim Food)</option>
              <option value="VOLUNTEER">Volunteer (Deliver Food)</option>
            </select>
          </div>

          <div className="form-group">
            <label>Location *</label>
            <button 
              type="button" 
              className="btn btn-secondary btn-sm"
              onClick={() => setShowMap(!showMap)}
            >
              {showMap ? 'Hide Map' : '📍 Select Location on Map'}
            </button>
            
            {formData.latitude && formData.longitude && (
              <p className="location-selected">
                ✓ Location selected: {formData.latitude.toFixed(6)}, {formData.longitude.toFixed(6)}
              </p>
            )}
            
            {showMap && (
              <div className="map-container">
                <LocationPicker 
                  onLocationSelect={handleLocationSelect}
                  height="250px"
                />
              </div>
            )}
          </div>
          
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Creating Account...' : 'Create Account'}
          </button>
        </form>
        
        <p className="auth-link">
          Already have an account? <Link to="/login">Sign in here</Link>
        </p>
      </div>
    </div>
  );
};

export default Register;
