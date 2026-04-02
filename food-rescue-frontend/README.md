# Food Rescue Frontend

React frontend for the Food Rescue application - connecting restaurants with NGOs to reduce food waste.

## Features

- **Role-based Dashboards**: Separate interfaces for Restaurants and NGOs
- **JWT Authentication**: Secure login with token-based auth
- **Restaurant Features**: Add food donations, view donation history
- **NGO Features**: Browse available food, claim donations, track claims
- **Modern UI**: Clean, responsive design with gradient accents

## Prerequisites

- Node.js 16+ 
- npm or yarn
- Running Food Rescue Backend at `http://localhost:8080`

## Setup & Installation

```bash
# Navigate to project folder
cd C:\Users\tarak\Desktop\Spring-boot\Springboots-projects\food-rescue-frontend

# Install dependencies
npm install

# Start development server
npm start
```

The app will open at `http://localhost:3000`

## Project Structure

```
food-rescue-frontend/
├── public/
│   └── index.html
├── src/
│   ├── components/
│   │   ├── Login.js
│   │   ├── Register.js
│   │   ├── RestaurantDashboard.js
│   │   └── NGODashboard.js
│   ├── context/
│   │   └── AuthContext.js
│   ├── services/
│   │   └── api.js
│   ├── App.js
│   ├── App.css
│   └── index.js
├── package.json
└── README.md
```

## Architecture

### Authentication Flow
1. User logs in via `/auth/login` endpoint
2. JWT token stored in `localStorage`
3. Axios interceptor adds token to all requests
4. AuthContext manages global auth state
5. Protected routes redirect unauthenticated users

### API Integration (`src/services/api.js`)
- **authService**: Login and register endpoints
- **restaurantService**: Add food, view restaurant's food list
- **ngoService**: View available food, claim food, view claims
- Axios interceptors handle JWT and CORS

### Role-Based Routing (`src/App.js`)
- Unauthenticated users → Login/Register pages
- Authenticated RESTAURANT → RestaurantDashboard
- Authenticated NGO → NGODashboard

## Pages

### Login (`/login`)
- Email and password form
- Link to registration page
- Redirects to dashboard on success

### Register (`/register`)
- Name, email, password form
- Role selection (Restaurant/NGO)
- Success message and redirect to login

### Restaurant Dashboard (`/dashboard`)
**Header**: User info, role badge, logout button

**Add Food Section**:
- Form: Food name, quantity, expiry date
- Submit adds food item via API
- Success/error messages displayed

**Food List Section**:
- Table of all food items added by this restaurant
- Columns: Food name, Quantity, Expiry date, Status
- Status badges: AVAILABLE (green) or CLAIMED (yellow)

### NGO Dashboard (`/dashboard`)
**Header**: User info, role badge, logout button

**Tabs**:
1. **Available Food**: Grid of claimable food items from all restaurants
   - Card displays: Food name, Restaurant name, Quantity, Expiry date
   - "Claim Food" button (disabled during claim request)

2. **My Claims**: Table of claims made by this NGO
   - Columns: Food item, Restaurant, Claim date, Status
   - Status badges: PENDING (blue) or APPROVED (green)

## Backend Integration

### API Endpoints Used

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/auth/login` | POST | No | Authenticate user |
| `/auth/register` | POST | No | Create new account |
| `/restaurant/add-food` | POST | Yes (Restaurant) | Add food donation |
| `/restaurant/food-list` | GET | Yes (Restaurant) | Get restaurant's food |
| `/food/available` | GET | Yes | List available food |
| `/food/claim/{id}` | POST | Yes (NGO) | Claim food item |
| `/food/my-claims` | GET | Yes (NGO) | Get NGO's claims |

### Request Headers
```
Authorization: Bearer <jwt_token>
Content-Type: application/json
```

### CORS
Backend configured to accept requests from `http://localhost:3000`

## Component Details

### AuthContext
```javascript
const { user, login, logout, isAuthenticated, isRestaurant, isNGO } = useAuth();
```
- Manages auth state from localStorage
- Provides role check helpers
- Handles login/logout

### API Service
```javascript
// Axios instance with interceptors
api.interceptors.request.use(...)  // Add JWT header
api.interceptors.response.use(...) // Handle 401 errors

// Service methods
authService.login(email, password)
restaurantService.addFood(foodData)
ngoService.claimFood(foodId)
```

## Testing the Application

### 1. Start Backend
Ensure Spring Boot backend is running at `http://localhost:8080`

### 2. Register Accounts
- Register as Restaurant: `/register` → Select "Restaurant"
- Register as NGO: `/register` → Select "NGO"

### 3. Restaurant Workflow
1. Login as Restaurant
2. Add food items with name, quantity, expiry date
3. View added food in the list
4. See status change when NGO claims food

### 4. NGO Workflow
1. Login as NGO
2. View "Available Food" tab
3. Click "Claim Food" on items
4. Check "My Claims" tab for claim status

## Styling

CSS uses:
- **Gradient theme**: Purple (#667eea) to pink (#764ba2)
- **Card-based layout**: White cards with subtle shadows
- **Status badges**: Color-coded (green=available/approved, yellow=claimed, blue=pending)
- **Responsive**: Grid layouts adapt to screen size

Key classes:
- `.auth-container` - Login/register page background
- `.dashboard-header` - Top navigation bar
- `.card` - Content sections
- `.food-grid` - NGO food listing
- `.data-table` - Tabular data display
- `.btn-primary` / `.btn-claim` - Action buttons

## Error Handling

- **401 Unauthorized**: Redirects to login page
- **API Errors**: Displayed as toast messages (auto-dismiss after 5s)
- **Form Validation**: HTML5 required attributes
- **Loading States**: Buttons disabled during API calls

## Security

- JWT tokens stored in localStorage
- Axios interceptors automatically attach tokens
- 401 responses clear storage and redirect to login
- Role-based UI prevents unauthorized actions

## Development Notes

- React 18 with functional components and hooks
- React Router v6 with `BrowserRouter`
- Context API for global state (AuthContext)
- No Redux needed for this app size
- CSS-in-JS not used (plain CSS file)

## Build for Production

```bash
npm run build
```

Creates optimized build in `build/` folder for deployment.

## Troubleshooting

**CORS errors**: Ensure backend is running and CORS is configured for `localhost:3000`

**401 errors**: Token may have expired; logout and login again

**API not responding**: Check backend is running on port 8080

**Port 3000 in use**: Change in package.json or use `PORT=3001 npm start`
