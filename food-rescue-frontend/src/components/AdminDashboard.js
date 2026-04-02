import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { adminService } from '../services/api';

const AdminDashboard = () => {
  const { user, logout } = useAuth();
  const [pendingNgos, setPendingNgos] = useState([]);
  const [stats, setStats] = useState(null);
  const [activeTab, setActiveTab] = useState('ngos');
  const [message, setMessage] = useState({ type: '', text: '' });
  const [actionLoading, setActionLoading] = useState(null);

  const fetchPendingNgos = useCallback(async () => {
    try {
      const response = await adminService.getPendingNgos();
      setPendingNgos(response.data);
    } catch (err) {
      showMessage('error', 'Failed to load pending NGOs');
    }
  }, []);

  const fetchStats = useCallback(async () => {
    try {
      const response = await adminService.getStats();
      setStats(response.data);
    } catch (err) {
      showMessage('error', 'Failed to load statistics');
    }
  }, []);

  useEffect(() => {
    fetchPendingNgos();
    fetchStats();
  }, [fetchPendingNgos, fetchStats]);

  const showMessage = (type, text) => {
    setMessage({ type, text });
    setTimeout(() => setMessage({ type: '', text: '' }), 5000);
  };

  const handleApproveNgo = async (ngoId) => {
    setActionLoading(ngoId);
    try {
      await adminService.approveNgo(ngoId);
      showMessage('success', 'NGO approved successfully!');
      fetchPendingNgos();
      fetchStats();
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to approve NGO');
    } finally {
      setActionLoading(null);
    }
  };

  const handleRejectNgo = async (ngoId) => {
    setActionLoading(ngoId);
    try {
      await adminService.rejectNgo(ngoId);
      showMessage('success', 'NGO rejected');
      fetchPendingNgos();
      fetchStats();
    } catch (err) {
      showMessage('error', err.response?.data?.message || 'Failed to reject NGO');
    } finally {
      setActionLoading(null);
    }
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div className="header-content">
          <h1>Admin Dashboard</h1>
          <div className="user-info">
            <span>Welcome, <strong>{user?.name}</strong></span>
            <span className="role-badge admin">{user?.role}</span>
            <button onClick={logout} className="btn btn-logout">Logout</button>
          </div>
        </div>
      </header>

      <div className="dashboard-content">
        {message.text && (
          <div className={`message ${message.type}`}>{message.text}</div>
        )}

        <div className="tabs">
          <button
            className={`tab-btn ${activeTab === 'ngos' ? 'active' : ''}`}
            onClick={() => setActiveTab('ngos')}
          >
            Pending NGOs ({pendingNgos.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'analytics' ? 'active' : ''}`}
            onClick={() => setActiveTab('analytics')}
          >
            Analytics
          </button>
        </div>

        {activeTab === 'ngos' && (
          <div className="card">
            <div className="section-header">
              <h2>Pending NGO Approvals</h2>
              <span className="badge">{pendingNgos.length} awaiting approval</span>
            </div>
            {pendingNgos.length === 0 ? (
              <p className="empty-message">No pending NGO approvals.</p>
            ) : (
              <div className="table-container">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>NGO Name</th>
                      <th>Email</th>
                      <th>Status</th>
                      <th>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {pendingNgos.map((ngo) => (
                      <tr key={ngo.id}>
                        <td>{ngo.name}</td>
                        <td>{ngo.email}</td>
                        <td>
                          <span className="status-badge status-pending">PENDING</span>
                        </td>
                        <td>
                          <button
                            onClick={() => handleApproveNgo(ngo.id)}
                            className="btn btn-approve"
                            disabled={actionLoading === ngo.id}
                          >
                            {actionLoading === ngo.id ? 'Processing...' : 'Approve'}
                          </button>
                          <button
                            onClick={() => handleRejectNgo(ngo.id)}
                            className="btn btn-reject"
                            disabled={actionLoading === ngo.id}
                          >
                            Reject
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {activeTab === 'analytics' && stats && (
          <div className="analytics-container">
            <div className="stats-grid">
              <div className="stat-card">
                <h4>Total Food Added</h4>
                <div className="stat-value">{stats.totalFoodAdded}</div>
              </div>
              <div className="stat-card">
                <h4>Total Food Claimed</h4>
                <div className="stat-value">{stats.totalFoodClaimed}</div>
              </div>
              <div className="stat-card">
                <h4>Total Food Expired</h4>
                <div className="stat-value">{stats.totalFoodExpired}</div>
              </div>
              <div className="stat-card highlight">
                <h4>Pending Claims</h4>
                <div className="stat-value">{stats.pendingClaimsCount}</div>
              </div>
            </div>

            <div className="card">
              <div className="section-header">
                <h2>Top Restaurants by Food Added</h2>
              </div>
              {stats.topRestaurants?.length === 0 ? (
                <p className="empty-message">No data available.</p>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Rank</th>
                        <th>Restaurant</th>
                        <th>Food Items Added</th>
                      </tr>
                    </thead>
                    <tbody>
                      {stats.topRestaurants?.map((restaurant, index) => (
                        <tr key={restaurant.id}>
                          <td>#{index + 1}</td>
                          <td>{restaurant.name}</td>
                          <td>{restaurant.count}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>

            <div className="card">
              <div className="section-header">
                <h2>Top NGOs by Claims Completed</h2>
              </div>
              {stats.topNgos?.length === 0 ? (
                <p className="empty-message">No data available.</p>
              ) : (
                <div className="table-container">
                  <table className="data-table">
                    <thead>
                      <tr>
                        <th>Rank</th>
                        <th>NGO</th>
                        <th>Claims Completed</th>
                      </tr>
                    </thead>
                    <tbody>
                      {stats.topNgos?.map((ngo, index) => (
                        <tr key={ngo.id}>
                          <td>#{index + 1}</td>
                          <td>{ngo.name}</td>
                          <td>{ngo.count}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminDashboard;
