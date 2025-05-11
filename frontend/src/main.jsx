// File: frontend/src/main.jsx
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import './index.css'; // Your global styles

import CssBaseline from '@mui/material/CssBaseline';
// import { ThemeProvider, createTheme } from '@mui/material/styles'; // We'll use this later for custom themes

// const theme = createTheme(); // Default theme

ReactDOM.createRoot(document.getElementById('root')).render(
    <React.StrictMode>
        {/* <ThemeProvider theme={theme}> */}
        <CssBaseline /> {/* Apply baseline styling */}
        <App />
        {/* </ThemeProvider> */}
    </React.StrictMode>,
);