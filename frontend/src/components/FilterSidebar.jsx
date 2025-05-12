// File: frontend/src/components/FilterSidebar.jsx

import React, { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import PropTypes from 'prop-types';
import { getAllAuthors, updateAuthor, deleteAuthor } from '../api/authorService';
import { getAllThemes, updateTheme, deleteTheme } from '../api/themeService';
import { getAllColors, updateColor, deleteColor } from '../api/colorService';
import { useAuth } from '../context/AuthContext.jsx';

// Material UI Imports...
import Box from '@mui/material/Box';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Collapse from '@mui/material/Collapse';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import TextField from '@mui/material/TextField';
import Snackbar from '@mui/material/Snackbar';
import MuiAlert from '@mui/material/Alert';
import Tooltip from '@mui/material/Tooltip';

// Icons...
import PeopleIcon from '@mui/icons-material/People';
import CategoryIcon from '@mui/icons-material/Category';
import PaletteIcon from '@mui/icons-material/Palette';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import LabelImportantIcon from '@mui/icons-material/LabelImportant';
import HomeIcon from '@mui/icons-material/Home';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import SaveIcon from '@mui/icons-material/Save';
import CancelIcon from '@mui/icons-material/Cancel';

const MAX_INITIAL_ITEMS = 5;

// Alert component remains the same...
const Alert = React.forwardRef(function Alert(props, ref) {
    return <MuiAlert elevation={6} ref={ref} variant="filled" {...props} />;
});
Alert.propTypes = {
    severity: PropTypes.oneOf(['error', 'warning', 'info', 'success']),
    children: PropTypes.node,
    onClose: PropTypes.func,
};
Alert.defaultProps = {
    severity: 'info',
    children: null,
    onClose: null,
};

// getApiServices remains the same...
const getApiServices = (type) => {
    // ... (implementation unchanged)
    if (type === 'author') {
        return { update: updateAuthor, delete: deleteAuthor, fetchAll: getAllAuthors };
    }
    if (type === 'theme') {
        return { update: updateTheme, delete: deleteTheme, fetchAll: getAllThemes };
    }
    if (type === 'color') {
        return { update: updateColor, delete: deleteColor, fetchAll: getAllColors };
    }
    return null;
};

function FilterableListSection({
                                   title,
                                   icon,
                                   onSelectItem,
                                   // Receive selectedItemId instead of name
                                   selectedItemId,
                                   filterType,
                                   onItemUpdatedOrDeleted,
                               }) {
    const [open, setOpen] = useState(false);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showAll, setShowAll] = useState(false);
    const [editingItemId, setEditingItemId] = useState(null);
    const [editValue, setEditValue] = useState('');
    const editInputRef = useRef(null);
    const [snackbar, setSnackbar] = useState({ open: false, message: '', severity: 'info' });

    const { hasRole } = useAuth();
    const isAdmin = hasRole('ROLE_ADMIN');

    const api = useMemo(() => getApiServices(filterType), [filterType]);

    // loadItems remains the same...
    const loadItems = useCallback(async () => {
        // ... (implementation unchanged)
        if (!api?.fetchAll) {
            const errorMessage = `API services or fetchAll not available for type: ${filterType}`;
            setError(errorMessage);
            setLoading(false);
            console.error(errorMessage);
            setSnackbar({ open: true, message: errorMessage, severity: 'error' });
            return;
        }
        try {
            setLoading(true);
            setError(null);
            const data = await api.fetchAll();
            const mappedItems = Array.isArray(data)
                ? data.map(item => ({ id: item.id, name: String(item.name || '') }))
                : [];
            const sortedItems = mappedItems.toSorted((a, b) => a.name.localeCompare(b.name));
            setItems(sortedItems);
        } catch (err) {
            const specificErrorMessage = err instanceof Error ? err.message : 'Unknown error';
            const generalErrorMessage = `Failed to load ${title?.toLowerCase() || 'items'}`;
            setError(generalErrorMessage);
            console.error(`${generalErrorMessage}: ${specificErrorMessage}`, err);
            setSnackbar({ open: true, message: `${generalErrorMessage}: ${specificErrorMessage}`, severity: 'error' });
        } finally {
            setLoading(false);
        }
    }, [api, title, filterType]);

    useEffect(() => {
        void loadItems();
    }, [loadItems]);

    useEffect(() => {
        if (editingItemId && editInputRef.current && isAdmin) {
            editInputRef.current.focus();
        }
    }, [editingItemId, isAdmin]);

    const handleClick = () => setOpen(!open);
    const handleToggleShowAll = (e) => { e.stopPropagation(); setShowAll(!showAll); };

    const handleEdit = (e, item) => {
        e.stopPropagation();
        if (!isAdmin) return;
        setEditingItemId(item.id);
        setEditValue(item.name);
    };

    const handleCancelEdit = (e) => {
        if (e) e.stopPropagation();
        setEditingItemId(null);
        setEditValue('');
    };

    // handleSaveEdit needs to pass the ID on update notification
    const handleSaveEdit = useCallback(async (e, itemId) => {
        if (e) e.stopPropagation();
        if (!isAdmin || !api?.update) return;
        if (!editValue.trim()) {
            setSnackbar({ open: true, message: 'Name cannot be empty.', severity: 'warning' });
            return;
        }
        const originalItem = items.find(i => i.id === itemId);
        if (originalItem?.name === editValue.trim()) {
            handleCancelEdit();
            return;
        }

        try {
            const updatedItem = await api.update(itemId, editValue.trim());
            setItems(prevItems =>
                prevItems
                    .map(i => (i.id === itemId ? { ...i, name: updatedItem.name } : i))
                    .toSorted((a, b) => a.name.localeCompare(b.name))
            );
            setEditingItemId(null);
            setSnackbar({ open: true, message: `${filterType || 'Item'} "${updatedItem.name}" updated.`, severity: 'success' });
            if (onItemUpdatedOrDeleted) {
                // Pass the ID for the updated item
                onItemUpdatedOrDeleted(filterType, updatedItem.id, 'update', updatedItem.name);
            }
        } catch (err) {
            const message = err instanceof Error ? err.message : 'An unknown error occurred';
            console.error(`Failed to update ${filterType}`, err);
            setSnackbar({ open: true, message: `Failed to update: ${message}`, severity: 'error' });
        }
    }, [isAdmin, api, editValue, items, filterType, onItemUpdatedOrDeleted]);

    // handleDelete needs to pass the ID on delete notification
    const handleDelete = async (e, item) => {
        e.stopPropagation();
        if (!isAdmin || !api?.delete) return;
        if (window.confirm(`Are you sure you want to delete ${filterType || 'item'} "${item.name}"? This might affect existing builds.`)) {
            try {
                await api.delete(item.id);
                setItems(prevItems => prevItems.filter(i => i.id !== item.id));
                setSnackbar({ open: true, message: `${filterType || 'Item'} "${item.name}" deleted.`, severity: 'success' });
                if (onItemUpdatedOrDeleted) {
                    // Pass the ID for the deleted item
                    onItemUpdatedOrDeleted(filterType, item.id, 'delete');
                }
            } catch (err) {
                const message = err instanceof Error ? err.message : 'An unknown error occurred';
                console.error(`Failed to delete ${filterType}`, err);
                setSnackbar({ open: true, message: `Failed to delete: ${message}`, severity: 'error' });
            }
        }
    };

    const handleEditInputChange = (e) => setEditValue(e.target.value);

    const handleEditInputKeyDown = (e, itemId) => {
        if (e.key === 'Enter') void handleSaveEdit(null, itemId);
        else if (e.key === 'Escape') handleCancelEdit(null);
    };

    const handleCloseSnackbar = (event, reason) => {
        if (reason === 'clickaway') return;
        setSnackbar(prev => ({ ...prev, open: false, message: '', severity: 'info' }));
    };

    const displayedItems = showAll ? items : items.slice(0, MAX_INITIAL_ITEMS);
    const titleText = <Typography variant="subtitle1" fontWeight="medium">{title}</Typography>;
    const showMoreText = showAll ? 'Show Less' : `Show More (${items.length - MAX_INITIAL_ITEMS})`;

    return (
        <>
            <ListItemButton onClick={handleClick} sx={{ py: 1.5 }}>
                <ListItemIcon sx={{ minWidth: 40 }}>{icon}</ListItemIcon>
                <ListItemText primary={titleText} />
                {open ? <ExpandLess /> : <ExpandMore />}
            </ListItemButton>
            <Collapse in={open} timeout="auto" unmountOnExit>
                <List component="div" disablePadding sx={{ pl: 2 }}>
                    {loading && ( <ListItemText primary={<CircularProgress size={20} sx={{ ml: 2, my: 1 }} />} /> )}
                    {error && !loading && ( <ListItemText primary={<Typography color="error" sx={{ml:2}}>{error}</Typography>} /> )}
                    {!loading && !error && displayedItems.map((item) => (
                        <ListItemButton
                            key={item.id}
                            // Update selected logic to use ID
                            selected={!editingItemId && selectedItemId === item.id}
                            // Pass ID and Name to onSelectItem
                            onClick={editingItemId === item.id && isAdmin ? (e) => e.stopPropagation() : () => onSelectItem(filterType, item.id, item.name)}
                            sx={{ pl: 4, py: 0.5, display: 'flex', alignItems: 'center' }}
                        >
                            {editingItemId === item.id && isAdmin ? (
                                <>
                                    <TextField
                                        inputRef={editInputRef}
                                        value={editValue}
                                        onChange={handleEditInputChange}
                                        onKeyDown={(e) => handleEditInputKeyDown(e, item.id)}
                                        onBlur={() => { void handleSaveEdit(null, item.id); }}
                                        variant="standard"
                                        size="small"
                                        fullWidth
                                        onClick={(e) => e.stopPropagation()}
                                        sx={{ mr: 1 }}
                                    />
                                    <Tooltip title="Save">
                                        <IconButton size="small" onClick={(e) => { void handleSaveEdit(e, item.id); }} color="primary" aria-label="save"><SaveIcon fontSize="inherit" /></IconButton>
                                    </Tooltip>
                                    <Tooltip title="Cancel">
                                        <IconButton size="small" onClick={handleCancelEdit} aria-label="cancel"><CancelIcon fontSize="inherit" /></IconButton>
                                    </Tooltip>
                                </>
                            ) : (
                                <>
                                    <ListItemIcon sx={{ minWidth: 30 }}><LabelImportantIcon fontSize="small" /></ListItemIcon>
                                    <ListItemText primary={<Typography variant="body2" sx={{ flexGrow: 1 }}>{item.name}</Typography>} />
                                    {isAdmin && (
                                        <>
                                            <Tooltip title="Edit">
                                                <IconButton size="small" onClick={(e) => handleEdit(e, item)} aria-label="edit" color="primary">
                                                    <EditIcon fontSize="inherit" />
                                                </IconButton>
                                            </Tooltip>
                                            <Tooltip title="Delete">
                                                <IconButton size="small" onClick={(e) => handleDelete(e, item)} color="error" aria-label="delete"><DeleteIcon fontSize="inherit" /></IconButton>
                                            </Tooltip>
                                        </>
                                    )}
                                </>
                            )}
                        </ListItemButton>
                    ))}
                    {!loading && !error && items.length > MAX_INITIAL_ITEMS && (
                        <ListItemButton onClick={handleToggleShowAll} sx={{ pl: 4, py: 0.5 }}>
                            <ListItemText
                                primary={showMoreText}
                                sx={{ '& .MuiListItemText-primary': { typography: 'caption', color: 'primary.main' } }}
                            />
                        </ListItemButton>
                    )}
                    {!loading && !error && items.length === 0 && (
                        <ListItemText
                            primary={<Typography sx={{ pl: 4, fontStyle: 'italic' }}>None found</Typography>}
                        />
                    )}
                </List>
            </Collapse>
            <Snackbar
                open={snackbar.open}
                autoHideDuration={4000}
                onClose={handleCloseSnackbar}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
            >
                <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </>
    );
}

FilterableListSection.propTypes = {
    title: PropTypes.string.isRequired,
    icon: PropTypes.element.isRequired,
    onSelectItem: PropTypes.func.isRequired,
    // Update prop name and type
    selectedItemId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    filterType: PropTypes.string.isRequired,
    onItemUpdatedOrDeleted: PropTypes.func.isRequired,
};

FilterableListSection.defaultProps = {
    // Update default prop
    selectedItemId: null,
};

function FilterSidebar({ onFilterChange, activeFilter, onItemUpdatedOrDeletedInApp }) {
    // Check if filter is active based on ID
    const allBuildsSelected = !activeFilter?.type && activeFilter?.id == null;

    // Update wrapper to pass ID
    const handleItemUpdateOrDeleteWrapper = (itemType, itemId, actionType, nameIfUpdated) => {
        if (onItemUpdatedOrDeletedInApp) {
            // Pass itemId instead of name/oldName for consistency
            onItemUpdatedOrDeletedInApp(itemType, itemId, actionType, nameIfUpdated);
        }
    };

    return (
        <Box
            sx={{
                width: '100%',
                maxWidth: { xs: '100%', sm: 280, md: 300 },
                bgcolor: 'background.paper',
                borderRight: { xs: 'none', sm: '1px solid' },
                borderColor: 'divider',
                height: { xs: 'auto', sm: 'calc(100vh - 64px)' },
                overflowY: 'auto',
                position: { xs: 'relative', sm: 'sticky' },
                top: { sm: '64px' },
                zIndex: { sm: (theme) => theme.zIndex.drawer },
            }}
        >
            <List component="nav" aria-labelledby="nested-list-subheader" sx={{ pt: 0 }}>
                <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Typography variant="h6" fontWeight="medium">Filter Builds</Typography>
                </Box>
                <Divider />
                {/* Update onClick to pass nulls for clearing filter */}
                <ListItemButton onClick={() => onFilterChange(null, null, null)} selected={allBuildsSelected} sx={{ py: 1.5 }}>
                    <ListItemIcon sx={{ minWidth: 40 }}><HomeIcon /></ListItemIcon>
                    <ListItemText primary={<Typography variant="subtitle1" fontWeight="medium">All Builds</Typography>} />
                </ListItemButton>
                <Divider />

                <FilterableListSection
                    title="Authors"
                    icon={<PeopleIcon />}
                    onSelectItem={onFilterChange}
                    // Pass ID for selection check
                    selectedItemId={activeFilter?.type === 'author' ? activeFilter.id : null}
                    filterType="author"
                    onItemUpdatedOrDeleted={handleItemUpdateOrDeleteWrapper}
                />
                <Divider />
                <FilterableListSection
                    title="Themes"
                    icon={<CategoryIcon />}
                    onSelectItem={onFilterChange}
                    // Pass ID for selection check
                    selectedItemId={activeFilter?.type === 'theme' ? activeFilter.id : null}
                    filterType="theme"
                    onItemUpdatedOrDeleted={handleItemUpdateOrDeleteWrapper}
                />
                <Divider />
                <FilterableListSection
                    title="Colors"
                    icon={<PaletteIcon />}
                    onSelectItem={onFilterChange}
                    // Pass ID for selection check
                    selectedItemId={activeFilter?.type === 'color' ? activeFilter.id : null}
                    filterType="color"
                    onItemUpdatedOrDeleted={handleItemUpdateOrDeleteWrapper}
                />
            </List>
        </Box>
    );
}

FilterSidebar.propTypes = {
    onFilterChange: PropTypes.func.isRequired,
    // Update PropTypes for activeFilter
    activeFilter: PropTypes.shape({
        type: PropTypes.string,
        id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
        name: PropTypes.string, // Keep name for display
    }),
    onItemUpdatedOrDeletedInApp: PropTypes.func.isRequired,
};

FilterSidebar.defaultProps = {
    // Update default prop
    activeFilter: { type: null, id: null, name: null },
};

export default FilterSidebar;