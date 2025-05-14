// File: frontend/src/context/AuthContext.jsx
// noinspection JSUnusedGlobalSymbols

import React, { createContext, useState, useContext, useEffect, useMemo } from 'react';
import PropTypes from 'prop-types'; // Import PropTypes
import {
    login as apiLogin,
    logout as apiLogout,
    getCurrentUser as apiGetCurrentUser
} from '../api/authService';

const AuthContext = createContext(null);

export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
    const [currentUser, setCurrentUser] = useState(null);
    const [isLoadingAuth, setIsLoadingAuth] = useState(true);
    const [authError, setAuthError] = useState(null);

    useEffect(() => {
        const checkLoggedInStatus = async () => {
            setIsLoadingAuth(true);
            setAuthError(null);
            try {
                const user = await apiGetCurrentUser();
                setCurrentUser(user);
            } catch (error) {
                console.error("Error during initial user check:", error);
                setCurrentUser(null);
            } finally {
                setIsLoadingAuth(false);
            }
        };
        checkLoggedInStatus();
    }, []); // Empty dependency array means this runs once on mount

    // Memoize login, logout, and setAuthError functions to stabilize the context value
    const login = useCallback(async (username, password) => {
        setIsLoadingAuth(true);
        setAuthError(null);
        try {
            await apiLogin(username, password); // Perform login
            // Fetch fresh user details after successful login to update context
            const freshUser = await apiGetCurrentUser();
            setCurrentUser(freshUser);
            setIsLoadingAuth(false);
            return freshUser;
        } catch (error) {
            console.error("Login failed in AuthContext:", error.message);
            setCurrentUser(null);
            setAuthError(error.message || 'Login failed. Please check your credentials.');
            setIsLoadingAuth(false);
            throw error;
        }
    }, []); // No dependencies that would change this function's identity often

    const logout = useCallback(async () => {
        setIsLoadingAuth(true);
        setAuthError(null);
        try {
            await apiLogout();
        } catch (error) {
            console.error("Logout API call failed in AuthContext:", error);
        } finally {
            setCurrentUser(null);
            try {
                // Refresh to get unauthenticated XSRF token for next potential login
                await apiGetCurrentUser();
            } catch (e) {
                console.error("Error calling getCurrentUser after logout:", e);
            }
            setIsLoadingAuth(false);
        }
    }, []); // No dependencies that would change this function's identity often

    // setAuthError is a state setter, already stable, but include if ESLint complains
    const stableSetAuthError = useCallback((error) => {
        setAuthError(error);
    }, []);


    const value = useMemo(() => ({
        currentUser,
        isLoadingAuth,
        authError,
        setAuthError: stableSetAuthError, // Use the memoized version
        login, // Already memoized with useCallback
        logout, // Already memoized with useCallback
        isAuthenticated: !!currentUser,
        hasRole: (role) => {
            if (!currentUser || (!currentUser.roles && !currentUser.authorities)) {
                return false;
            }
            // Ensure rolesToCheck is always an array
            const rolesToCheck = currentUser.authorities ?
                currentUser.authorities.map(auth =>
                    (typeof auth === 'string' ? auth : auth?.authority)
                ) :
                (currentUser.roles || []); // Fallback to empty array if roles also undefined
            return rolesToCheck.some(r => r === role);
        }
    }), [currentUser, isLoadingAuth, authError, login, logout, stableSetAuthError]);
    // Dependencies for useMemo: only recompute if these values change.

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};

// Add PropTypes validation for AuthProvider
AuthProvider.propTypes = {
    children: PropTypes.node.isRequired,
};