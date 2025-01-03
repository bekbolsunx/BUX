# B U X: Expense Tracker App

B U X is an Android application designed to help users manage their personal finances efficiently. 
The app allows users to track their expenses and income, visualize data with dynamic charts, and customize 
settings such as receiving notifications to log transactions. Built with a user-friendly interface and powerful 
backend integrations.

---

## Features

### Core Functionalities

1. **Expense and Income Tracking**:
    - Add, edit, and delete transactions.
    - View expenses and income by category (e.g., Food, Travel, etc.).
    - Dynamic filters for transactions by Day, Week, Month, Year, or Custom Period.
2. **Data Visualization**:
    - Pie chart representation of expenses and income by category.
    - Summed-up totals displayed dynamically for each selected period.
3. **Total Balance Management**:
    - Tracks the user’s remaining balance based on transactions.
    - Syncs with Firebase Firestore for real-time updates.

### Additional Features

1. **Google Sign-In**:
    - Easy authentication with Google accounts.
    - Profile picture integration from Google or placeholder.
2. **Notifications**:
    - Customizable notifications to remind users to log transactions every 12 hours.
    - Ability to enable/disable reminders from the settings page.
3. **Settings Page**:
    - Toggle notifications on/off.
    - Manage preferences for reminders.
4. **Responsive Design**:
    - Consistent light/dark mode experience using Material Design 3.
    - Fixed theme across all devices, unaffected by system-wide preferences.

---

## Tech Stack

### Frontend

- **Kotlin**: Android programming language.
- **XML**: For UI layout design.
- **Material Design 3**: For consistent UI elements.

### Backend

- **Firebase Firestore**: Real-time database for user data and transactions.
- **Firebase Authentication**: User authentication (Email & Google Sign-In).
- **WorkManager**: Handles background notifications and reminders.

### Libraries

- **MPAndroidChart**: For visualizing data in charts.
- **Google Play Services Auth**: For Google Sign-In functionality.
