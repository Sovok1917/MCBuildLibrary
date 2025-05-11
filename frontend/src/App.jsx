import React, { useState, useEffect, useCallback } from 'react';
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
                <Typography sx={{ml: 2}}>Checking authentication...</Typography>
            </Box>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" state={{ from: location }} replace />;
    }
    return children;
};

function AppContent() {
    const [builds, setBuilds] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editingBuild, setEditingBuild] = useState(null);
    const [isFormVisible, setIsFormVisible] = useState(false);
    const [activeFilter, setActiveFilter] = useState({ type: null, name: null });
    const { isAuthenticated, hasRole } = useAuth();

    const buildListRightEdgeVisualInset = 12;

    const fetchBuilds = useCallback(async () => {
        try {
            setIsLoading(true);
            setError(null);
            const filterParams = {};
            if (activeFilter.type && activeFilter.name) {
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
        fetchBuilds();
    }, [fetchBuilds]);

    const handleFilterChange = (type, name) => {
        setActiveFilter({ type, name });
        if (!editingBuild) {
            setIsFormVisible(false);
        }
    };

    const handleItemUpdatedOrDeletedInApp = (itemType, itemName, actionType) => {
        if (activeFilter.type === itemType && activeFilter.name === itemName) {
            if (actionType === 'delete') {
                setActiveFilter({ type: null, name: null });
            }
        }
        fetchBuilds();
    };

    const handleShowCreateForm = () => {
        const initialDataForForm = {};
        if (activeFilter.type && activeFilter.name) {
            if (activeFilter.type === 'author') initialDataForForm.authorNames = activeFilter.name;
            if (activeFilter.type === 'theme') initialDataForForm.themeNames = activeFilter.name;
            if (activeFilter.type === 'color') initialDataForForm.colorNames = activeFilter.name;
        }
        setEditingBuild({ ...initialDataForForm });
        setIsFormVisible(true);
    };

    const handleFormSubmitSuccess = () => {
        fetchBuilds();
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
        fetchBuilds(); // Refetch builds after one is deleted
        if (editingBuild && builds.find(b => b.id === editingBuild.id)) {
            // If the build being edited was deleted, close the form
            // This check might be redundant if deletion always closes form or navigates
        }
    };


    return (
        <Box sx={{ display: 'flex', mt: '64px' }}>
            <FilterSidebar
                onFilterChange={handleFilterChange}
                activeFilter={activeFilter}
                onItemUpdatedOrDeletedInApp={handleItemUpdatedOrDeletedInApp}
            />
            <Container
                component="main"
                maxWidth="lg"
                sx={{ flexGrow: 1, p: { xs: 1, sm: 2, md: 3 }, overflowY: 'auto' }}
            >
                <Grid
                    container
                    spacing={0}
                    alignItems="center"
                    justifyContent="space-between"
                    sx={{ mb: 2, mt: { xs: 1, sm: 0 } }}
                >
                    <Grid item xs>
                        <Box sx={{ display: 'flex', alignItems: 'center' }}>
                            <Typography variant="h4" component="h2">
                                Minecraft Builds
                            </Typography>
                            {activeFilter.type && activeFilter.name && (
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
                        sx={{ paddingRight: (theme) => theme.spacing(buildListRightEdgeVisualInset / 8) }} // Assuming theme.spacing(1) = 8px
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
                        {isFormVisible && editingBuild && editingBuild.id && hasRole('ROLE_ADMIN') && (
                            <Typography variant="subtitle1" component="div" sx={{ display: 'flex', alignItems: 'center', color: 'primary.main', whiteSpace: 'nowrap' }}>
                                <EditNoteIcon sx={{ mr: 0.5 }} /> Editing Build...
                            </Typography>
                        )}
                        {isFormVisible && (!editingBuild || !editingBuild.id) && isAuthenticated && (
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
                            marginRight: (theme) => theme.spacing(buildListRightEdgeVisualInset / 8),
                        }}
                    >
                        <BuildForm
                            key={editingBuild ? `edit-${editingBuild.id || 'newFiltered'}` : 'create'}
                            onBuildCreated={handleFormSubmitSuccess}
                            existingBuild={editingBuild}
                            onBuildUpdated={handleFormSubmitSuccess}
                            onCancelForm={handleCancelForm}
                        />
                    </Box>
                </Collapse>

                {isLoading && (<Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>)}
                {error && (<Alert severity="error" sx={{ mt: 3 }}>Error fetching builds: {error}</Alert>)}
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

function App() {
    const { isAuthenticated, logout, isLoadingAuth, currentUser } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    // This top-level loading state is crucial for not rendering routes prematurely
    if (isLoadingAuth) {
        return (
            <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', flexDirection: 'column' }}>
                <CircularProgress size={60} />
                <Typography variant="h6" sx={{mt: 2}}>Loading Application...</Typography>
            </Box>
        );
    }

    return (
        <>
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar>
                    <Button component={RouterLink} to="/" color="inherit" startIcon={<HomeIcon />} sx={{mr: 2}}>
                        <Typography variant="h6" component="div" sx={{ flexGrow: 1, display: {xs: 'none', sm: 'block'} }}>
                            MC Builds
                        </Typography>
                    </Button>
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1, display: {xs: 'block', sm: 'none'} }}>
                        MC Builds
                    </Typography>


                    {isAuthenticated ? (
                        <>
                            <Typography sx={{mr: 2, display: {xs: 'none', sm: 'block'}}}>
                                Welcome, {currentUser?.username}
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
                    path="/*" // Catch-all for protected content
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

export default App;