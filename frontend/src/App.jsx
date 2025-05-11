// File: frontend/src/App.jsx
import React, { useState, useEffect, useCallback } from 'react';
import BuildList from './components/BuildList';
import BuildForm from './components/BuildForm';
import FilterSidebar from './components/FilterSidebar';
import { getFilteredBuilds } from './api/buildService';

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
import Chip from '@mui/material/Chip'; // <--- ADD THIS IMPORT
import AddCircleOutlineIcon from '@mui/icons-material/AddCircleOutline';
import EditNoteIcon from '@mui/icons-material/EditNote';
import FilterListIcon from '@mui/icons-material/FilterList';

function App() {
    const [builds, setBuilds] = useState([]);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState(null);
    const [editingBuild, setEditingBuild] = useState(null);
    const [isFormVisible, setIsFormVisible] = useState(false);
    const [activeFilter, setActiveFilter] = useState({ type: null, name: null });

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
        setEditingBuild(buildToEdit);
        setIsFormVisible(true);
        // window.scrollTo({ top: 0, behavior: 'smooth' }); // Already at top of content area
    };

    return (
        <>
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar>
                    <Typography variant="h6" component="div" sx={{ flexGrow: 1 }}>
                        Minecraft Build Library
                    </Typography>
                </Toolbar>
            </AppBar>

            <Box sx={{ display: 'flex', mt: '64px' }}>
                <FilterSidebar onFilterChange={handleFilterChange} activeFilter={activeFilter} />

                <Container component="main" maxWidth="lg" sx={{ flexGrow: 1, p: 3, overflowY: 'auto' }}>
                    <Box
                        sx={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            mb: 2,
                        }}
                    >
                        <Box sx={{display: 'flex', alignItems: 'center'}}>
                            <Typography variant="h4" component="h2">
                                Minecraft Builds
                            </Typography>
                            {activeFilter.type && activeFilter.name && (
                                <Chip // Now this Chip component is defined due to the import
                                    icon={<FilterListIcon />}
                                    label={`${activeFilter.type}: ${activeFilter.name}`}
                                    onDelete={() => handleFilterChange(null, null)}
                                    color="primary"
                                    variant="outlined"
                                    sx={{ ml: 2 }}
                                />
                            )}
                        </Box>
                        {!isFormVisible && (
                            <Button
                                variant="contained"
                                startIcon={<AddCircleOutlineIcon />}
                                onClick={handleShowCreateForm}
                            >
                                Add New Build
                            </Button>
                        )}
                        {isFormVisible && editingBuild && editingBuild.id && (
                            <Typography variant="h6" component="div" sx={{ display: 'flex', alignItems: 'center', color: 'primary.main' }}>
                                <EditNoteIcon sx={{ mr: 1 }} /> Editing Build...
                            </Typography>
                        )}
                        {isFormVisible && (!editingBuild || !editingBuild.id) && (
                            <Typography variant="h6" component="div" sx={{ display: 'flex', alignItems: 'center', color: 'secondary.main' }}>
                                <AddCircleOutlineIcon sx={{ mr: 1 }} /> Create New Build
                            </Typography>
                        )}
                    </Box>

                    <Collapse in={isFormVisible} timeout="auto" unmountOnExit>
                        <Box sx={{ mb: 3, boxShadow: 3, borderRadius: 1, p: 0 }}>
                            <BuildForm
                                key={editingBuild ? `edit-${editingBuild.id || 'newFiltered'}` : 'create'}
                                onBuildCreated={handleFormSubmitSuccess}
                                existingBuild={editingBuild}
                                onBuildUpdated={handleFormSubmitSuccess}
                                onCancelForm={handleCancelForm}
                            />
                        </Box>
                    </Collapse>

                    {isLoading && (
                        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}><CircularProgress /></Box>
                    )}
                    {error && (
                        <Alert severity="error" sx={{ mt: 3 }}>Error fetching builds: {error}</Alert>
                    )}
                    {!isLoading && !error && (
                        <BuildList
                            builds={builds}
                            onBuildDeleted={fetchBuilds}
                            onEditBuild={handleEditBuild}
                        />
                    )}
                </Container>
            </Box>
        </>
    );
}

export default App;