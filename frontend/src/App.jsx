// File: frontend/src/App.jsx
// noinspection JSUnusedGlobalSymbols

import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { Routes, Route, Link as RouterLink, useNavigate, Navigate, useLocation } from 'react-router-dom';
import BuildList from './components/BuildList';
import BuildForm from './components/BuildForm';
import FilterSidebar from './components/FilterSidebar';
import Login from './components/Login';
import Register from './components/Register'; // <-- Import Register component
import { getFilteredBuilds, getBuildsByRelatedEntity } from './api/buildService';
import { useAuth } from './context/AuthContext.jsx';
import { useDebounce } from './hooks/useDebounce';

// Material UI Imports... (remain the same)
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
import TextField from '@mui/material/TextField';
import InputAdornment from '@mui/material/InputAdornment';
import IconButton from '@mui/material/IconButton';

// Icons... (remain the same)
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import EditNoteIcon from '@mui/icons-material/EditNote';
import FilterListIcon from '@mui/icons-material/FilterList';
import LoginIcon from '@mui/icons-material/Login';
import LogoutIcon from '@mui/icons-material/Logout';
import HomeIcon from '@mui/icons-material/Home';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';
import PersonAddIcon from '@mui/icons-material/PersonAdd'; // <-- Icon for Register button

/**
 * ProtectedRoute component remains the same...
 */
const ProtectedRoute = ({ children }) => {
    // ... (implementation remains the same)
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

ProtectedRoute.propTypes = {
    children: PropTypes.node.isRequired,
};


/**
 * AppContent component remains the same...
 */
function AppContent({ searchQuery, setSearchQuery }) {
    // ... (implementation remains the same)
    const [builds, setBuilds] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editingBuild, setEditingBuild] = useState(null);
    const [isFormVisible, setIsFormVisible] = useState(false);
    const [activeFilter, setActiveFilter] = useState({ type: null, id: null, name: null });
    const { isAuthenticated, hasRole } = useAuth();
    const debouncedSearchQuery = useDebounce(searchQuery, 500);

    const buildListRightEdgeVisualInset = 46;

    const parseSearchQuery = (query) => {
        // ... (implementation remains the same)
        const params = { name: null, author: null, theme: null, color: null };
        if (!query || query.trim() === '') {
            return params;
        }

        let remainingQuery = query.trim();
        const keywords = ['author', 'theme', 'color'];
        keywords.forEach(keyword => {
            const regex = new RegExp(`(?:^|\\s)${keyword}:\\s*([^:]+?)(?=\\s+(?:author:|theme:|color:)|\\s*$)`, 'i');
            let match;
            // eslint-disable-next-line no-cond-assign
            while ((match = regex.exec(remainingQuery)) !== null) {
                if (match[1]?.trim()) {
                    params[keyword] = match[1].trim();
                    remainingQuery = remainingQuery.substring(0, match.index) + remainingQuery.substring(regex.lastIndex);
                    remainingQuery = remainingQuery.trim();
                    regex.lastIndex = 0;
                    break;
                }
            }
        });

        if (remainingQuery) {
            params.name = remainingQuery;
        }

        console.log("Parsed search params:", params);
        return params;
    };


    const fetchBuilds = useCallback(async () => {
        // ... (implementation remains the same)
        try {
            setIsLoading(true);
            setError(null);
            let data;
            const trimmedDebouncedQuery = debouncedSearchQuery.trim();

            if (trimmedDebouncedQuery !== '') {
                const searchParams = parseSearchQuery(trimmedDebouncedQuery);
                const hasValidSearchParam = Object.values(searchParams).some(val => val);

                if (hasValidSearchParam) {
                    console.log(`Fetching builds using parsed search:`, searchParams);
                    data = await getFilteredBuilds(searchParams);
                } else {
                    console.log("Search query parsed to no valid terms, fetching all builds.");
                    data = await getFilteredBuilds({});
                }
                if (activeFilter.type || activeFilter.id) {
                    setActiveFilter({ type: null, id: null, name: null });
                }
            } else if (activeFilter?.type && activeFilter?.id != null) {
                console.log(`Fetching builds by related entity: ${activeFilter.type}, ID: ${activeFilter.id}`);
                data = await getBuildsByRelatedEntity(activeFilter.type, activeFilter.id);
            } else {
                console.log("Fetching all builds (no active filter or search).");
                data = await getFilteredBuilds({});
            }
            setBuilds(data);
        } catch (err) {
            console.error("Error fetching builds:", err);
            setError(err.message);
            setBuilds([]);
        } finally {
            setIsLoading(false);
        }
    }, [activeFilter, debouncedSearchQuery]);

    useEffect(() => {
        void fetchBuilds();
    }, [fetchBuilds]);

    const handleFilterChange = (type, identifier, name = null) => {
        // ... (implementation remains the same)
        setSearchQuery('');
        if (type === null) {
            setActiveFilter({ type: null, id: null, name: null });
        } else {
            setActiveFilter({ type, id: identifier, name });
        }
        if (!editingBuild) {
            setIsFormVisible(false);
        }
    };

    const handleItemUpdatedOrDeletedInApp = (itemType, itemId, actionType) => {
        // ... (implementation remains the same)
        if (activeFilter?.type === itemType && activeFilter?.id === itemId) {
            console.log(`Active filter item (${itemType} ID: ${itemId}) was ${actionType}d. Clearing filter.`);
            setActiveFilter({ type: null, id: null, name: null });
            setSearchQuery('');
        } else {
            console.log(`Item (${itemType} ID: ${itemId}) was ${actionType}d. Refetching builds.`);
            void fetchBuilds();
        }
    };

    const handleShowCreateForm = () => {
        // ... (implementation remains the same)
        const initialDataForForm = {};
        if (!searchQuery && activeFilter?.type && activeFilter?.name) {
            if (activeFilter.type === 'author') initialDataForForm.authorNames = activeFilter.name;
            if (activeFilter.type === 'theme') initialDataForForm.themeNames = activeFilter.name;
            if (activeFilter.type === 'color') initialDataForForm.colorNames = activeFilter.name;
        }
        setEditingBuild({ ...initialDataForForm });
        setIsFormVisible(true);
    };

    const handleFormSubmitSuccess = () => {
        // ... (implementation remains the same)
        void fetchBuilds();
        setEditingBuild(null);
        setIsFormVisible(false);
    };

    const handleCancelForm = () => {
        // ... (implementation remains the same)
        setEditingBuild(null);
        setIsFormVisible(false);
    };

    const handleEditBuild = (buildToEdit) => {
        // ... (implementation remains the same)
        if (hasRole('ROLE_ADMIN')) {
            setEditingBuild(buildToEdit);
            setIsFormVisible(true);
        } else {
            alert("You don't have permission to edit builds.");
        }
    };

    const handleBuildDeleted = (deletedBuildId) => {
        // ... (implementation remains the same)
        if (editingBuild?.id === deletedBuildId) {
            setEditingBuild(null);
            setIsFormVisible(false);
        }
        void fetchBuilds();
    };

    const getChipLabel = () => {
        // ... (implementation remains the same)
        if (!activeFilter?.type || !activeFilter?.name) {
            return '';
        }
        const baseLabel = `${activeFilter.type}: ${activeFilter.name}`;
        return hasRole('ROLE_ADMIN') ? `${baseLabel} (ID: ${activeFilter.id})` : baseLabel;
    };

    return (
        <Box sx={{ display: 'flex', mt: '64px' }}> {/* AppBar height */}
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
                {/* Grid for Title, Chips, Buttons */}
                <Grid
                    container
                    spacing={0}
                    alignItems="center"
                    justifyContent="space-between"
                    sx={{ mb: 2, mt: { xs: 1, sm: 0 } }}
                >
                    {/* Title and Chips */}
                    <Grid item xs>
                        <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap' }}>
                            <Typography variant="h4" component="h2" sx={{ mr: 2, mb: { xs: 1, sm: 0 } }}>
                                Minecraft Builds
                            </Typography>
                            {!searchQuery && activeFilter?.type && activeFilter?.name && (
                                <Chip
                                    icon={<FilterListIcon />}
                                    label={getChipLabel()}
                                    onDelete={() => handleFilterChange(null, null, null)}
                                    color="primary"
                                    variant="outlined"
                                    sx={{ display: { xs: 'none', sm: 'flex' } }}
                                />
                            )}
                            {searchQuery && (
                                <Chip
                                    icon={<SearchIcon />}
                                    label={`Search: "${searchQuery}"`}
                                    onDelete={() => setSearchQuery('')}
                                    color="secondary"
                                    variant="outlined"
                                    sx={{ ml: (!activeFilter?.type || searchQuery) ? 2 : 0, display: { xs: 'none', sm: 'flex' } }}
                                />
                            )}
                        </Box>
                    </Grid>
                    {/* Action Buttons/Indicators */}
                    <Grid
                        item
                        xs="auto"
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

                {/* Build Form Collapse */}
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
                            key={editingBuild?.id || 'create'}
                            onBuildCreated={handleFormSubmitSuccess}
                            existingBuild={editingBuild}
                            onBuildUpdated={handleFormSubmitSuccess}
                            onCancelForm={handleCancelForm}
                        />
                    </Box>
                </Collapse>

                {/* Loading/Error/BuildList */}
                {isLoading && (<Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>)}
                {error && (<Alert severity="error" sx={{ mt: 3 }}>{error}</Alert>)}
                {!isLoading && !error && (
                    <BuildList
                        builds={builds}
                        onBuildDeleted={handleBuildDeleted}
                        onEditBuild={handleEditBuild}
                    />
                )}
                {!isLoading && !error && builds.length === 0 && (searchQuery || activeFilter.id) && (
                    <Alert severity="info" sx={{ mt: 3 }}>
                        No builds found matching your {searchQuery ? 'search query' : 'filter'}.
                    </Alert>
                )}
            </Container>
        </Box>
    );
}

AppContent.propTypes = {
    searchQuery: PropTypes.string.isRequired,
    setSearchQuery: PropTypes.func.isRequired,
};

/**
 * App component is the root component that sets up routing and the main AppBar.
 * It manages global states like authentication and search query.
 */
function App() {
    const { isAuthenticated, logout, isLoadingAuth, currentUser } = useAuth();
    const navigate = useNavigate();
    const [searchQuery, setSearchQuery] = useState('');

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    const handleSearchChange = (event) => {
        setSearchQuery(event.target.value);
    };

    const clearSearch = () => {
        setSearchQuery('');
    };

    const searchPlaceholder = "Name or author:bob theme:fantasy color:red";

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
                    {/* Home Button */}
                    <Button component={RouterLink} to="/" color="inherit" startIcon={<HomeIcon />} sx={{ mr: 1 }}>
                        <Typography variant="h6" component="div" sx={{ display: { xs: 'none', sm: 'block' } }}>
                            MC Builds
                        </Typography>
                    </Button>
                    <Typography variant="h6" component="div" sx={{ display: {xs: 'block', sm: 'none'}, mr: 1 }}>
                        MC Builds
                    </Typography>

                    {/* Search Bar (only when authenticated) */}
                    {isAuthenticated && (
                        <Box sx={{ position: 'relative', borderRadius: 1, backgroundColor: 'rgba(255, 255, 255, 0.15)', '&:hover': { backgroundColor: 'rgba(255, 255, 255, 0.25)' }, mr: 2, flexGrow: { xs: 1, sm: 0 }, width: { xs: 'auto', sm: 'auto' }, minWidth: { sm: '380px'} }}>
                            <TextField
                                placeholder={searchPlaceholder}
                                variant="standard"
                                fullWidth
                                value={searchQuery}
                                onChange={handleSearchChange}
                                startAdornment={
                                    <InputAdornment position="start">
                                        <SearchIcon sx={{ color: 'common.white', ml: 1 }} />
                                    </InputAdornment>
                                }
                                endAdornment={searchQuery ? (
                                    <InputAdornment position="end">
                                        <IconButton
                                            aria-label="clear search"
                                            onClick={clearSearch}
                                            edge="end"
                                            size="small"
                                            sx={{ color: 'common.white', mr: 1 }}
                                        >
                                            <ClearIcon fontSize="small"/>
                                        </IconButton>
                                    </InputAdornment>
                                ) : null}
                                disableUnderline
                                sx={{
                                    '& .MuiInput-root': { color: 'common.white', padding: '6px 10px', marginTop: 0 },
                                    '& .MuiInputBase-input': { paddingTop: '2px', paddingBottom: '2px', '&::placeholder': { color: 'rgba(255, 255, 255, 0.6)', opacity: 1, fontSize: '0.875rem' } },
                                }}
                            />
                        </Box>
                    )}

                    <Box sx={{ flexGrow: 1 }} /> {/* Spacer */}

                    {/* Auth Buttons */}
                    {isAuthenticated ? (
                        <>
                            <Typography sx={{ mr: 2, display: { xs: 'none', sm: 'block' } }}>
                                Welcome, {currentUser?.username}
                            </Typography>
                            <Button color="inherit" startIcon={<LogoutIcon />} onClick={handleLogout}>
                                Logout
                            </Button>
                        </>
                    ) : (
                        <>
                            {/* Show Register button next to Login */}
                            <Button color="inherit" startIcon={<PersonAddIcon />} component={RouterLink} to="/register" sx={{ mr: 1 }}>
                                Register
                            </Button>
                            <Button color="inherit" startIcon={<LoginIcon />} component={RouterLink} to="/login">
                                Login
                            </Button>
                        </>
                    )}
                </Toolbar>
            </AppBar>

            {/* Routes */}
            <Routes>
                {/* Redirect authenticated users away from login/register */}
                <Route path="/login" element={isAuthenticated ? <Navigate to="/" replace /> : <Login />} />
                <Route path="/register" element={isAuthenticated ? <Navigate to="/" replace /> : <Register />} />
                {/* Protected main content route */}
                <Route
                    path="/*"
                    element={
                        <ProtectedRoute>
                            <AppContent searchQuery={searchQuery} setSearchQuery={setSearchQuery} />
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </>
    );
}

export default App;