# NutriSense - AI-Powered Kitchen Assistant

A modern web application that uses AI to detect ingredients from fridge photos, provide personalized nutritional recommendations, suggest nearby stores for shopping, and recommend recipes based on available ingredients.

## Features

### Intelligent Ingredient Detection
- Upload or drag-and-drop fridge photos
- AI-powered ingredient recognition with confidence scores
- Freshness tracking (Fresh, Use Soon, Expired)
- Manual confirmation and editing capabilities
- Visual indicators for detection confidence levels

### Personalized User Profiles
- Age, gender, weight, and height tracking
- Health goal selection (Maintain Weight, Lose Weight, Gain Muscle, Increase Energy)
- Diet type preferences (Balanced, Vegetarian, Vegan, Keto, Paleo, Mediterranean)
- Customizable taste preferences with keyword tags

### Smart Shopping Recommendations
- Nutritional recommendations based on user profile and current ingredients
- Nearby store listings with distance information
- Price comparison across multiple retailers
- Grouped shopping lists by store for convenience

### Recipe Suggestions
- Personalized recipe recommendations based on available ingredients
- Cuisine and taste preference filtering
- Recipe metadata including cook time, difficulty, and calorie information
- Visual recipe cards with ingredient lists

## Technology Stack

- **HTML5** - Semantic markup and structure
- **CSS3** - Modern styling with gradients, animations, and responsive design
- **Vanilla JavaScript** - No framework dependencies for optimal performance
- **LocalStorage API** - Client-side data persistence

## Project Structure

```
frontend/
├── src
    ├── index.html          # Main HTML structure
    ├── styles.css          # Complete styling and responsive design
    ├── script.js           # Application logic and state management
└── README.md           # Project documentation
```

## Installation

1. Clone or download the project files
2. Ensure all three files (`index.html`, `styles.css`, `script.js`) are in the same directory
3. Open `index.html` in a modern web browser

No build process or dependencies required.

## Usage

### Getting Started

1. **Scan Your Fridge**
   - Navigate to the "Scan Fridge" tab
   - Click the upload area or drag and drop a photo of your fridge
   - Wait for AI processing (simulated with 2-second delay)
   - Review detected ingredients with confidence scores

2. **Confirm or Edit Ingredients**
   - Each detected ingredient shows:
     - Name, quantity, and unit
     - AI confidence percentage (color-coded)
     - Freshness status
   - Options for each ingredient:
     - **Confirm** - Accept the AI detection
     - **Edit** - Modify details in a popup modal
     - **Delete** - Remove from the list
   - Use "Confirm All" to quickly accept all detections

3. **Set Up Your Profile**
   - Navigate to the "Profile" tab
   - Enter your personal information (age, gender, weight, height)
   - Select your health goal and diet type
   - Add taste preferences (e.g., "spicy", "Italian", "comfort food")
   - Click "Save Profile"

4. **View Shopping Recommendations**
   - Navigate to the "Shopping List" tab
   - Review personalized ingredient recommendations
   - See nearby stores with pricing and distance
   - View items grouped by store for efficient shopping

5. **Discover Recipes**
   - Navigate to the "Recipes" tab
   - Browse recipes tailored to your ingredients and preferences
   - View recipe details including time, difficulty, and calories

### Confidence Score Guide

- 🟢 **85-100%** (Green) - High confidence detection
- 🟡 **70-84%** (Yellow) - Medium confidence, review recommended
- 🔴 **Below 70%** (Red) - Low confidence, verification needed

## Data Storage

All user data is stored locally in the browser using the LocalStorage API:
- User profile information
- Taste preferences
- No ingredients are persisted (fresh scan required each session)

**Privacy Note**: No data is sent to external servers in this frontend implementation.

## Responsive Design

The application is fully responsive and optimized for:
- Desktop (1200px+)
- Tablet (768px - 1199px)
- Mobile (< 768px)

## Future Backend Integration

This frontend is designed to integrate with a FastAPI/Flask backend that will provide:

### Expected API Endpoints

```
POST /api/detect-ingredients
- Input: Image file
- Output: Array of detected ingredients with confidence scores

POST /api/recommendations
- Input: User profile + current ingredients
- Output: Recommended ingredients with store/price data

POST /api/recipes
- Input: Available ingredients + user preferences
- Output: Matching recipes with metadata
```

### Integration Points

The following functions in `script.js` are ready for API integration:

- `simulateIngredientDetection()` - Replace with actual vision API call
- `generateRecommendations()` - Replace with nutrition recommendation API
- `generateRecipes()` - Replace with recipe recommendation API
- Store location/pricing API integration

## Customization

### Styling

Modify CSS variables in `styles.css` to customize the color scheme:

```css
:root {
    --primary-color: #10b981;      /* Main brand color */
    --secondary-color: #14b8a6;    /* Secondary accent */
    --accent-color: #06b6d4;       /* Additional accent */
}
```

### Execution
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope Process

cd ./backend/

.\.venv\Scripts\activate

uvicorn app.main:app --reload

## Known Limitations

- Ingredient detection is currently simulated (requires backend AI integration)
- Store locations and prices are mock data (requires maps/pricing APIs)
- Recipe database is limited (requires recipe API integration)
- No user authentication (single-user application)