// File: frontend/src/App.jsx
// noinspection JSUnusedGlobalSymbols

import React, { useState, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';
import { Routes, Route, Link as RouterLink, useNavigate, Navigate, useLocation } from 'react-router-dom';
import BuildList from './components/BuildList';
import BuildForm from './components/BuildForm';
import FilterSidebar from './components/FilterSidebar';
import Login from './components/Login';
import { getFilteredBuilds, getBuildsByRelatedEntity } from './api/buildService';
import { useAuth } from './context/AuthContext.jsx';
import { useDebounce } from './hooks/useDebounce';

// Material UI Imports
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

// Icons
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import EditNoteIcon from '@mui/icons-material/EditNote';
import FilterListIcon from '@mui/icons-material/FilterList';
import LoginIcon from '@mui/icons-material/Login';
import LogoutIcon from '@mui/icons-material/Logout';
import HomeIcon from '@mui/icons-material/Home';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';

/**
 * ProtectedRoute component ensures that only authenticated users can access its children.
 * It displays a loading indicator while checking authentication status.
 * If not authenticated, it redirects to the login page.
 */
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

ProtectedRoute.propTypes = {
    children: PropTypes.node.isRequired,
};

/**
 * AppContent component is the main layout for the authenticated part of the application.
 * It manages fetching and displaying builds, handling filters, search, and build forms.
 * @param {object} props - The component's props.
 * @param {string} props.searchQuery - The current search query string.
 * @param {function} props.setSearchQuery - Function to update the search query.
 */
function AppContent({ searchQuery, setSearchQuery }) {
    const [builds, setBuilds] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editingBuild, setEditingBuild] = useState(null);
    const [isFormVisible, setIsFormVisible] = useState(false);
    const [activeFilter, setActiveFilter] = useState({ type: null, id: null, name: null });
    const { isAuthenticated, hasRole } = useAuth();
    const debouncedSearchQuery = useDebounce(searchQuery, 500); // 500ms debounce delay

    // This constant might be used for responsive padding/margin calculations.
    // If not, it can be removed.
    const buildListRightEdgeVisualInset = 111;

    /**
     * Parses the raw search query into specific filter parameters using RegExp.exec().
     * Keywords: author:, theme:, color:
     * Any text not part of a keyword phrase is treated as a build name search.
     * @param {string} query The raw search query string.
     * @returns {object} An object with keys { name, author, theme, color },
     *                   where values can be null if not specified.
     */
    const parseSearchQuery = (query) => {
        const params = { name: null, author: null, theme: null, color: null };
        if (!query || query.trim() === '') {
            return params;
        }

        let remainingQuery = query.trim();
        const keywords = ['author', 'theme', 'color'];
        keywords.forEach(keyword => {
            // Use non-capturing group (?:...) for start/space and lookahead
            const regex = new RegExp(`(?:^|\\s)${keyword}:\\s*([^:]+?)(?=\\s+(?:author:|theme:|color:)|\\s*$)`, 'i');
            let match;
            // Use exec() in a loop in case the keyword appears multiple times (though logic takes first)
            // eslint-disable-next-line no-cond-assign
            while ((match = regex.exec(remainingQuery)) !== null) {
                // Check if match[1] (the captured value) exists and is not just whitespace
                if (match[1]?.trim()) {
                    params[keyword] = match[1].trim();
                    // Remove the matched part (including keyword and value) from the remaining query
                    // Use match.index to replace accurately
                    remainingQuery = remainingQuery.substring(0, match.index) + remainingQuery.substring(regex.lastIndex);
                    remainingQuery = remainingQuery.trim(); // Trim again after removal
                    regex.lastIndex = 0; // Reset lastIndex for next potential match in loop (though unlikely with current logic)
                    break; // Assuming only one instance per keyword is needed
                }
            }
        });

        // If keywords were found, the remaining text is the name.
        // If no keywords were found, the entire query is the name.
        if (remainingQuery) {
            params.name = remainingQuery;
        }

        console.log("Parsed search params:", params);
        return params;
    };


    const fetchBuilds = useCallback(async () => {
        try {
            setIsLoading(true);
            setError(null);
            let data; // Declare data without initializing
            const trimmedDebouncedQuery = debouncedSearchQuery.trim();

            if (trimmedDebouncedQuery !== '') {
                const searchParams = parseSearchQuery(trimmedDebouncedQuery);
                // Check if any parameter (name, author, theme, color) has a non-null, non-empty value
                const hasValidSearchParam = Object.values(searchParams).some(val => val);

                if (hasValidSearchParam) {
                    console.log(`Fetching builds using parsed search:`, searchParams);
                    data = await getFilteredBuilds(searchParams); // Pass all params
                } else {
                    console.log("Search query parsed to no valid terms, fetching all builds.");
                    data = await getFilteredBuilds({}); // Fetch all if parsing yields nothing
                }
                // Clear active sidebar filter when a text search is performed
                if (activeFilter.type || activeFilter.id) {
                    setActiveFilter({ type: null, id: null, name: null });
                }
            } else if (activeFilter?.type && activeFilter?.id != null) {
                console.log(`Fetching builds by related entity: ${activeFilter.type}, ID: ${activeFilter.id}`);
                data = await getBuildsByRelatedEntity(activeFilter.type, activeFilter.id);
            } else {
                console.log("Fetching all builds (no active filter or search).");
                data = await getFilteredBuilds({}); // Fetch all if no search and no filter
            }
            setBuilds(data);
        } catch (err) {
            console.error("Error fetching builds:", err);
            setError(err.message);
            setBuilds([]); // Set to empty array on error
        } finally {
            setIsLoading(false);
        }
    }, [activeFilter, debouncedSearchQuery]); // parseSearchQuery is stable

    useEffect(() => {
        void fetchBuilds();
    }, [fetchBuilds]);

    const handleFilterChange = (type, identifier, name = null) => {
        setSearchQuery(''); // Clear search query when a sidebar filter is applied
        if (type === null) {
            setActiveFilter({ type: null, id: null, name: null });
        } else {
            setActiveFilter({ type, id: identifier, name });
        }
        if (!editingBuild) { // Close form only if not editing
            setIsFormVisible(false);
        }
    };

    const handleItemUpdatedOrDeletedInApp = (itemType, itemId, actionType) => {
        // If the deleted/updated item was the active filter, clear the filter and search
        if (activeFilter?.type === itemType && activeFilter?.id === itemId) {
            console.log(`Active filter item (${itemType} ID: ${itemId}) was ${actionType}d. Clearing filter.`);
            setActiveFilter({ type: null, id: null, name: null });
            setSearchQuery('');
        } else {
            // Otherwise, just refetch builds as the list might have changed
            console.log(`Item (${itemType} ID: ${itemId}) was ${actionType}d. Refetching builds.`);
            void fetchBuilds();
        }
    };

    const handleShowCreateForm = () => {
        const initialDataForForm = {};
        // Pre-fill form based on active filter only if search is empty
        if (!searchQuery && activeFilter?.type && activeFilter?.name) {
            if (activeFilter.type === 'author') initialDataForForm.authorNames = activeFilter.name;
            if (activeFilter.type === 'theme') initialDataForForm.themeNames = activeFilter.name;
            if (activeFilter.type === 'color') initialDataForForm.colorNames = activeFilter.name;
        }
        setEditingBuild({ ...initialDataForForm });
        setIsFormVisible(true);
    };

    const handleFormSubmitSuccess = () => {
        void fetchBuilds();
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

    const handleBuildDeleted = (deletedBuildId) => {
        if (editingBuild?.id === deletedBuildId) {
            setEditingBuild(null);
            setIsFormVisible(false);
        }
        void fetchBuilds();
    };

    const getChipLabel = () => {
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
                <Grid
                    container
                    spacing={0}
                    alignItems="center"
                    justifyContent="space-between"
                    sx={{ mb: 2, mt: { xs: 1, sm: 0 } }}
                >
                    <Grid item xs>
                        <Box sx={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap' }}>
                            <Typography variant="h4" component="h2" sx={{ mr: 2, mb: { xs: 1, sm: 0 } }}>
                                Minecraft Builds
                            </Typography>
                            {/* Show filter chip only if filter is active AND search is empty */}
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
                            {/* Show search chip if search is active */}
                            {searchQuery && (
                                <Chip
                                    icon={<SearchIcon />}
                                    label={`Search: "${searchQuery}"`}
                                    onDelete={() => setSearchQuery('')}
                                    color="secondary"
                                    variant="outlined"
                                    // Adjust margin if filter chip is also potentially visible (though logic prevents it now)
                                    sx={{ ml: (!activeFilter?.type || searchQuery) ? 2 : 0, display: { xs: 'none', sm: 'flex' } }}
                                />
                            )}
                        </Box>
                    </Grid>
                    <Grid
                        item
                        xs="auto"
                        // eslint-disable-next-line
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

                <Collapse in={isFormVisible && isAuthenticated} timeout="auto" unmountOnExit>
                    <Box
                        sx={{
                            mb: 3,
                            boxShadow: 3,
                            borderRadius: 1,
                            // eslint-disable-next-line
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
                    <Button component={RouterLink} to="/" color="inherit" startIcon={<HomeIcon />} sx={{ mr: 1 }}>
                        <Typography variant="h6" component="div" sx={{ display: { xs: 'none', sm: 'block' } }}>
                            MC Builds
                        </Typography>
                    </Button>
                    <Typography variant="h6" component="div" sx={{ display: {xs: 'block', sm: 'none'}, mr: 1 }}>
                        MC Builds
                    </Typography>

                    {isAuthenticated && (
                        <Box sx={{ position: 'relative', borderRadius: 1, backgroundColor: 'rgba(255, 255, 255, 0.15)', '&:hover': { backgroundColor: 'rgba(255, 255, 255, 0.25)' }, mr: 2, flexGrow: { xs: 1, sm: 0 }, width: { xs: 'auto', sm: 'auto' }, minWidth: { sm: '380px'} }}>
                            {/* Refactored TextField: Removed InputProps, passed props directly */}
                            <TextField
                                placeholder={searchPlaceholder}
                                variant="standard"
                                fullWidth
                                value={searchQuery}
                                onChange={handleSearchChange}
                                // Pass adornments directly
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
                                // Pass disableUnderline directly
                                disableUnderline
                                // Apply styling directly via sx, targeting the input base and input itself
                                sx={{
                                    // Style the root input element (MuiInput-root for standard variant)
                                    '& .MuiInput-root': {
                                        color: 'common.white',
                                        padding: '6px 10px', // Padding for the whole input area
                                        marginTop: 0, // Override default margin if needed
                                    },
                                    // Style the actual input HTML element
                                    '& .MuiInputBase-input': {
                                        paddingTop: '2px', // Fine-tune input padding
                                        paddingBottom: '2px',
                                        // Style the placeholder within the input
                                        '&::placeholder': {
                                            color: 'rgba(255, 255, 255, 0.6)',
                                            opacity: 1, // Ensure placeholder is visible
                                            fontSize: '0.875rem',
                                        },
                                    },
                                }}
                            />
                        </Box>
                    )}

                    <Box sx={{ flexGrow: 1 }} /> {/* Spacer */}

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
                            <AppContent searchQuery={searchQuery} setSearchQuery={setSearchQuery} />
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </>
    );
}

export default App;