import axios from 'axios';

const API_URL = 'http://localhost:8080';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export const authService = {
  login: (email, password) => api.post('/auth/login', { email, password }),
  register: (name, email, password, role, latitude, longitude) => api.post('/auth/register', { name, email, password, role, latitude, longitude }),
};

export const restaurantService = {
  addFood: (foodData) => api.post('/restaurant/add-food', foodData),
  getFoodList: () => api.get('/restaurant/food-list'),
  getPendingClaims: () => api.get('/restaurant/pending-claims'),
  approveClaim: (claimId) => api.put(`/restaurant/approve-claim/${claimId}`),
  rejectClaim: (claimId, reason) => api.put(`/restaurant/reject-claim/${claimId}`, { reason }),
};

export const ngoService = {
  getAvailableFood: () => api.get('/food/available'),
  claimFood: (foodId) => api.post(`/food/claim/${foodId}`),
  getMyClaims: () => api.get('/food/my-claims'),
  getNearbyDonations: (lat, lon, radiusKm = 10) => api.get(`/food/nearby?lat=${lat}&lon=${lon}&radiusKm=${radiusKm}`),
  pickupFood: (claimId) => api.post(`/food/pickup/${claimId}`),
  getRoute: (foodItemId) => api.get(`/food/route/${foodItemId}`),
};

export const adminService = {
  getPendingNgos: () => api.get('/admin/pending-ngos'),
  approveNgo: (ngoId) => api.put(`/admin/approve-ngo/${ngoId}`),
  rejectNgo: (ngoId) => api.put(`/admin/reject-ngo/${ngoId}`),
  getStats: () => api.get('/admin/stats'),
};

export const volunteerService = {
  getMyAssignments: () => api.get('/volunteer/my-assignments'),
  pickupDonation: (donationId) => api.put(`/volunteer/donations/${donationId}/pickup`),
  deliverDonation: (donationId) => api.put(`/volunteer/donations/${donationId}/deliver`),
};

export const userService = {
  updateLocation: (latitude, longitude) => api.put('/api/users/update-location', { latitude, longitude }),
  getProfile: () => api.get('/api/users/profile'),
};

export default api;
