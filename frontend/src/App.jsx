// File: frontend/src/App.jsx
// noinspection JSUnusedGlobalSymbols

import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types'; // Import PropTypes
import { Routes, Route, Link as RouterLink, useNavigate, Navigate, useLocation } from 'react-router-dom';
import BuildList from './components/BuildList';
import BuildForm from './components/BuildForm';
import FilterSidebar from './components/FilterSidebar';
import Login from './components/Login';
import { getFilteredBuilds } from './api/buildService';
import { useAuth } from './context/AuthContext.jsx';

import Container from '@mui/material/Container';
import Typography from '@mui/material/Typography';
import AppBar from '@mui/material/AppBar';
import Toolbar from '@mui/material/Toolbar';
import CircularProgress from '@mui/material/CircularProgress';
import Box from '@mui/material/Box';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Collapse from '@mui/material/Collapse';
import Grid from '@mui/material/Grid';
import Chip from '@mui/material/Chip';
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import EditNoteIcon from '@mui/icons-material/EditNote';
import FilterListIcon from '@mui/icons-material/FilterList';
import LoginIcon from '@mui/icons-material/Login';
import LogoutIcon from '@mui/icons-material/Logout';
import HomeIcon from '@mui/icons-material/Home';

const ProtectedRoute = ({ children }) => {
    const { isAuthenticated, isLoadingAuth } = useAuth();
    const location = useLocation();

    if (isLoadingAuth) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 'calc(100vh - 64px)', mt: '64px' }}>
                <CircularProgress />
                <Typography sx={{ ml: 2 }}>Checking authentication...</Typography>
            </Box>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }
    return children;
};

// Define propTypes for ProtectedRoute
ProtectedRoute.propTypes = {
    children: PropTypes.node.isRequired, // 'children' is a required React node
};

function AppContent() {
    const [builds, setBuilds] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editingBuild, setEditingBuild] = useState(null);
    const [isFormVisible, setIsFormVisible] = useState(false);
    const [activeFilter, setActiveFilter] = useState({ type: null, name: null });
    const { isAuthenticated, hasRole } = useAuth();

    // This constant was defined but its usage in sx props was flagged as unused.
    // If it's truly unused visually, it can be removed.
    // If it IS used and the linter is wrong, the warning can be ignored or sx prop adjusted.
    // For now, keeping it as it might be part of a larger theme.
    const buildListRightEdgeVisualInset = 111;

    const fetchBuilds = useCallback(async () => {
        try {
            setIsLoading(true);
            setError(null);
            const filterParams = {};
            if (activeFilter?.type && activeFilter?.name) { // Optional chaining for safety
                filterParams[activeFilter.type] = activeFilter.name;
            }
            const data = await getFilteredBuilds(filterParams);
            setBuilds(data);
        } catch (err) {
            setError(err.message);
            setBuilds([]);
        } finally {
            setIsLoading(false);
        }
    }, [activeFilter]);

    useEffect(() => {
        void fetchBuilds(); // Explicitly ignore promise
    }, [fetchBuilds]);

    const handleFilterChange = (type, name) => {
        setActiveFilter({ type, name });
        if (!editingBuild) { // Check if editingBuild is not null before accessing properties
            setIsFormVisible(false);
        }
    };

    const handleItemUpdatedOrDeletedInApp = (itemType, itemName, actionType) => {
        if (activeFilter?.type === itemType && activeFilter?.name === itemName) {
            if (actionType === 'delete') {
                setActiveFilter({ type: null, name: null });
            }
        }
        void fetchBuilds(); // Explicitly ignore promise
    };

    const handleShowCreateForm = () => {
        const initialDataForForm = {};
        if (activeFilter?.type && activeFilter?.name) {
            if (activeFilter.type === 'author') initialDataForForm.authorNames = activeFilter.name;
            if (activeFilter.type === 'theme') initialDataForForm.themeNames = activeFilter.name;
            if (activeFilter.type === 'color') initialDataForForm.colorNames = activeFilter.name;
        }
        setEditingBuild({ ...initialDataForForm }); // Not null, so no optional chaining needed here
        setIsFormVisible(true);
    };

    const handleFormSubmitSuccess = () => {
        void fetchBuilds(); // Explicitly ignore promise
        setEditingBuild(null);
        setIsFormVisible(false);
    };

    const handleCancelForm = () => {
        setEditingBuild(null);
        setIsFormVisible(false);
    };

    const handleEditBuild = (buildToEdit) => {
        if (hasRole('ROLE_ADMIN')) {
            setEditingBuild(buildToEdit);
            setIsFormVisible(true);
        } else {
            alert("You don't have permission to edit builds.");
        }
    };

    const handleBuildDeleted = () => {
        void fetchBuilds(); // Explicitly ignore promise
        // The check for editingBuild.id was potentially problematic if editingBuild was null.
        // This logic might need refinement based on exact desired behavior.
        // If a build is deleted, and it was the one being edited, the form should likely close.
        // `setEditingBuild(null)` and `setIsFormVisible(false)` might be appropriate here
        // if the deleted build matches `editingBuild?.id`.
    };

    return (
        <Box sx={{ display: 'flex', mt: '64px' }}> {/* AppBar height */}
            <FilterSidebar
                onFilterChange={handleFilterChange}
                activeFilter={activeFilter} // PropType for activeFilter in FilterSidebar should allow nulls
                onItemUpdatedOrDeletedInApp={handleItemUpdatedOrDeletedInApp}
            />
            <Container
                component="main"
                maxWidth="lg"
                sx={{ flexGrow: 1, p: { xs: 1, sm: 2, md: 3 }, overflowY: 'auto' }}
            >
                <Grid
                    container
                    spacing={0} // Or some spacing like {xs: 1, sm: 2}
                    alignItems="center"
                    justifyContent="space-between"
                    sx={{ mb: 2, mt: { xs: 1, sm: 0 } }}
                >
                    <Grid item xs>
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <Typography variant="h4" component="h2">
                                Minecraft Builds
                            </Typography>
                            {/* Optional chaining for activeFilter properties */}
                            {activeFilter?.type && activeFilter?.name && (
                                <Chip
                                    icon={<FilterListIcon />}
                                    label={`${activeFilter.type}: ${activeFilter.name}`}
                                    onDelete={() => handleFilterChange(null, null)}
                                    color="primary"
                                    variant="outlined"
                                    sx={{ ml: 2, display: { xs: 'none', sm: 'flex' } }}
                                />
                            )}
                        </Box>
                    </Grid>
                    <Grid
                        item
                        xs="auto"
                        // The warning "Unused property paddingRight" might mean this sx prop
                        // isn't having the desired effect or is overridden. Review visual output.
                        // If it's needed, the warning can be ignored or addressed by ensuring it applies.
                        // eslint-disable-next-line <Unused>
                        sx={{ paddingRight: (theme) => theme.spacing(buildListRightEdgeVisualInset / 8) }}
                    >
                        {isAuthenticated && !isFormVisible && (
                            <Button
                                variant="contained"
                                startIcon={<AddCircleOutlineIcon />}
                                onClick={handleShowCreateForm}
                            >
                                Add New Build
                            </Button>
                        )}
                        {/* Optional chaining for editingBuild properties */}
                        {isFormVisible && editingBuild?.id && hasRole('ROLE_ADMIN') && (
                            <Typography variant="subtitle1" component="div" sx={{ display: 'flex', alignItems: 'center', color: 'primary.main', whiteSpace: 'nowrap' }}>
                                <EditNoteIcon sx={{ mr: 0.5 }} /> Editing Build...
                            </Typography>
                        )}
                        {isFormVisible && !editingBuild?.id && isAuthenticated && (
                            <Typography variant="subtitle1" component="div" sx={{ display: 'flex', alignItems: 'center', color: 'secondary.main', whiteSpace: 'nowrap' }}>
                                <AddCircleOutlineIcon sx={{ mr: 0.5 }} /> Create New Build
                            </Typography>
                        )}
                    </Grid>
                </Grid>

                <Collapse in={isFormVisible && isAuthenticated} timeout="auto" unmountOnExit>
                    <Box
                        sx={{
                            mb: 3,
                            boxShadow: 3,
                            borderRadius: 1,
                            // The warning "Unused property marginRight" might mean this sx prop
                            // isn't having the desired effect. Review visual output.
                            marginRight: (theme) => theme.spacing(buildListRightEdgeVisualInset / 8),
                        }}
                    >
                        <BuildForm
                            key={editingBuild?.id || 'create'} // Use optional chaining for key
                            onBuildCreated={handleFormSubmitSuccess}
                            existingBuild={editingBuild}
                            onBuildUpdated={handleFormSubmitSuccess}
                            onCancelForm={handleCancelForm}
                        />
                    </Box>
                </Collapse>

                {isLoading && (<Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /> {/* Valid ReactNode */}</Box>)}
                {error && (<Alert severity="error" sx={{ mt: 3 }}>{error} {/* error is a string, valid */}</Alert>)}
                {!isLoading && !error && (
                    <BuildList
                        builds={builds}
                        onBuildDeleted={handleBuildDeleted}
                        onEditBuild={handleEditBuild}
                    />
                )}
            </Container>
        </Box>
    );
}
// AppContent does not receive props directly that need PropTypes here,
// as it gets its data from hooks and state.

function App() {
    const { isAuthenticated, logout, isLoadingAuth, currentUser } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout(); // The promise from logout() in authService is intentionally not used further here
        navigate('/login');
    };

    if (isLoadingAuth) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', flexDirection: 'column' }}>
                <CircularProgress size={60} />
                <Typography variant="h6" sx={{ mt: 2 }}>Loading Application...</Typography>
            </Box>
        );
    }

    return (
        <>
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar>
                    <Button component={RouterLink} to="/" color="inherit" startIcon={<HomeIcon />} sx={{ mr: 2 }}>
                        <Typography variant="h6" component="div" sx={{ flexGrow: 1, display: { xs: 'none', sm: 'block' } }}>
                            MC Builds
                        </Typography>
                    </Button>
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1, display: {xs: 'block', sm: 'none'} }}>
                        MC Builds {/* Simplified for mobile, ensure flexGrow behaves as expected */}
                    </Typography>

                    {isAuthenticated ? (
                        <>
                            <Typography sx={{ mr: 2, display: { xs: 'none', sm: 'block' } }}>
                                Welcome, {currentUser?.username} {/* Optional chaining */}
                            </Typography>
                            <Button color="inherit" startIcon={<LogoutIcon />} onClick={handleLogout}>
                                Logout
                            </Button>
                        </>
                    ) : (
                        <Button color="inherit" startIcon={<LoginIcon />} component={RouterLink} to="/login">
                            Login
                        </Button>
                    )}
                </Toolbar>
            </AppBar>

            <Routes>
                <Route path="/login" element={isAuthenticated ? <Navigate to="/" replace /> : <Login />} />
                <Route
                    path="/*"
                    element={
                        <ProtectedRoute>
                            <AppContent />
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </>
    );
}
// App does not receive props that need PropTypes here.

export default App;