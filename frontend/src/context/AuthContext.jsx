// File: frontend/src/context/AuthContext.jsx
import React, { createContext, useState, useContext, useEffect } from 'react';
import { login as apiLogin, logout as apiLogout, getCurrentUser } from '../api/authService'; // Ensure authService path is correct

const AuthContext = createContext(null);

// Make sure this line is exactly: export const useAuth = ...
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
                const user = await getCurrentUser();
                if (user) {
                    setCurrentUser(user);
                } else {
                    setCurrentUser(null);
                }
            } catch (error) {
                setCurrentUser(null);
            } finally {
                setIsLoadingAuth(false);
            }
        };
        checkLoggedInStatus();
    }, []);

    const login = async (username, password) => {
        setIsLoadingAuth(true);
        setAuthError(null);
        try {
            const user = await apiLogin(username, password);
            setCurrentUser(user);
            return user;
        } catch (error) {
            console.error("Login failed in AuthContext:", error);
            setCurrentUser(null);
            setAuthError(error.message || 'Login failed. Please check your credentials.');
            throw error;
        } finally {
            setIsLoadingAuth(false);
        }
    };

    const logout = async () => {
        setIsLoadingAuth(true);
        setAuthError(null);
        try {
            await apiLogout();
        } catch (error) {
            console.error("Logout API call failed in AuthContext:", error);
        } finally {
            setCurrentUser(null);
            setIsLoadingAuth(false);
        }
    };

    const value = {
        currentUser,
        isLoadingAuth,
        authError,
        setAuthError,
        login,
        logout,
        isAuthenticated: !!currentUser,
        hasRole: (role) => {
            if (!currentUser || (!currentUser.roles && !currentUser.authorities)) {
                return false;
            }
            const rolesToCheck = currentUser.authorities ?
                currentUser.authorities.map(auth => typeof auth === 'string' ? auth : auth.authority) :
                currentUser.roles;
            return rolesToCheck?.some(r => r === role);
        }
    };

    return (
        <AuthContext.Provider value={value}>
            {children}
        </AuthContext.Provider>
    );
};