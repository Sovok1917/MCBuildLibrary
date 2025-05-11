// File: frontend/src/components/Login.jsx
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext.jsx'; // Ensure .jsx if you renamed
import { useNavigate, useLocation } from 'react-router-dom';

import TextField from '@mui/material/TextField';
import Button from '@mui/material/Button';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';
import Paper from '@mui/material/Paper';
import Container from '@mui/material/Container'; // <-- ADD THIS IMPORT
import Link from '@mui/material/Link';

function Login() {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const { login, isLoadingAuth, authError, setAuthError } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    useEffect(() => {
        // Clear auth error when component mounts or location changes (e.g., navigating to login)
        // to prevent showing stale errors from other attempts.
        setAuthError(null);
    }, [setAuthError, location.pathname]); // Depend on location.pathname to reset on navigation to /login


    const handleSubmit = async (event) => {
        event.preventDefault();
        setAuthError(null);
        try {
            await login(username, password);
            const from = location.state?.from?.pathname || "/";
            navigate(from, { replace: true });
        } catch (error) {
            // error is set in AuthContext, no need to set it here again
            console.error('Login component submission error:', error.message);
        }
    };

    return (
        <Container component="main" maxWidth="xs">
            <Paper elevation={3} sx={{ p: { xs: 2, sm: 4 }, mt: 8, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                <Typography component="h1" variant="h5">
                    Sign In
                </Typography>
                <Box
                    component="form"
                    onSubmit={handleSubmit}
                    noValidate
                    sx={{ mt: 1, width: '100%' }}
                >
                    {authError && <Alert severity="error" sx={{ width: '100%', mb: 2 }}>{authError}</Alert>}
                    <TextField
                        margin="normal"
                        required
                        fullWidth
                        id="username"
                        label="Username"
                        name="username"
                        autoComplete="username"
                        autoFocus
                        value={username}
                        onChange={(e) => setUsername(e.target.value)}
                        disabled={isLoadingAuth}
                    />
                    <TextField
                        margin="normal"
                        required
                        fullWidth
                        name="password"
                        label="Password"
                        type="password"
                        id="password"
                        autoComplete="current-password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        disabled={isLoadingAuth}
                    />
                    <Button
                        type="submit"
                        fullWidth
                        variant="contained"
                        sx={{ mt: 3, mb: 2 }}
                        disabled={isLoadingAuth}
                        startIcon={isLoadingAuth ? <CircularProgress size={20} color="inherit" /> : null}
                    >
                        {isLoadingAuth ? 'Signing In...' : 'Sign In'}
                    </Button>
                </Box>
            </Paper>
        </Container>
    );
}

export default Login;