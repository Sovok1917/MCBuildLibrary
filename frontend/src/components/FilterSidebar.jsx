// File: frontend/src/components/FilterSidebar.jsx
import React, { useState, useEffect, useCallback, useRef } from 'react';
import { getAllAuthors, updateAuthor, deleteAuthor } from '../api/authorService';
import { getAllThemes, updateTheme, deleteTheme } from '../api/themeService';
import { getAllColors, updateColor, deleteColor } from '../api/colorService';
import { useAuth } from '../context/AuthContext.jsx';

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

const Alert = React.forwardRef(function Alert(props, ref) {
    return <MuiAlert elevation={6} ref={ref} variant="filled" {...props} />;
});

const getApiServices = (type) => {
    if (type === 'author') return { update: updateAuthor, delete: deleteAuthor, fetchAll: getAllAuthors };
    if (type === 'theme') return { update: updateTheme, delete: deleteTheme, fetchAll: getAllThemes };
    if (type === 'color') return { update: updateColor, delete: deleteColor, fetchAll: getAllColors };
    return null;
};

function FilterableListSection({
                                   title,
                                   icon,
                                   fetchItems,
                                   onSelectItem,
                                   selectedItemName,
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

    const api = getApiServices(filterType);

    const loadItems = useCallback(async () => {
        try {
            setLoading(true);
            setError(null);
            const data = await fetchItems();
            setItems(data.map(item => ({ id: item.id, name: item.name })).sort((a, b) => a.name.localeCompare(b.name)));
        } catch (err) {
            setError(`Failed to load ${title.toLowerCase()}`);
            console.error(err);
            setSnackbar({ open: true, message: `Error loading ${title}: ${err.message}`, severity: 'error' });
        } finally {
            setLoading(false);
        }
    }, [fetchItems, title]);

    useEffect(() => {
        loadItems();
    }, [loadItems]);

    useEffect(() => {
        if (editingItemId && editInputRef.current && isAdmin) {
            editInputRef.current.focus();
            editInputRef.current.select();
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

    const handleSaveEdit = async (e, itemId) => {
        if (e) e.stopPropagation();
        if (!isAdmin) return;
        if (!editValue.trim()) {
            setSnackbar({ open: true, message: 'Name cannot be empty.', severity: 'warning' });
            return;
        }
        const originalItem = items.find(i => i.id === itemId);
        if (originalItem && originalItem.name === editValue.trim()) {
            handleCancelEdit();
            return;
        }

        try {
            const updatedItem = await api.update(itemId, editValue.trim());
            const oldName = originalItem ? originalItem.name : null;
            setItems(prevItems => prevItems.map(i => (i.id === itemId ? { ...i, name: updatedItem.name } : i))
                .sort((a, b) => a.name.localeCompare(b.name)));
            setEditingItemId(null);
            setSnackbar({ open: true, message: `${filterType} "${updatedItem.name}" updated.`, severity: 'success' });
            if (onItemUpdatedOrDeleted) onItemUpdatedOrDeleted(filterType, itemId, updatedItem.name, 'update', oldName);
        } catch (err) {
            console.error(`Failed to update ${filterType}`, err);
            setSnackbar({ open: true, message: `Failed to update: ${err.message}`, severity: 'error' });
        }
    };

    const handleDelete = async (e, item) => {
        e.stopPropagation();
        if (!isAdmin) return;
        if (window.confirm(`Are you sure you want to delete ${filterType} "${item.name}"? This might affect existing builds.`)) {
            try {
                await api.delete(item.id);
                setItems(prevItems => prevItems.filter(i => i.id !== item.id));
                setSnackbar({ open: true, message: `${filterType} "${item.name}" deleted.`, severity: 'success' });
                if (onItemUpdatedOrDeleted) onItemUpdatedOrDeleted(filterType, item.id, item.name, 'delete');
            } catch (err) {
                console.error(`Failed to delete ${filterType}`, err);
                setSnackbar({ open: true, message: `Failed to delete: ${err.message}`, severity: 'error' });
            }
        }
    };

    const handleEditInputChange = (e) => setEditValue(e.target.value);
    const handleEditInputKeyDown = (e, itemId) => {
        if (e.key === 'Enter') handleSaveEdit(null, itemId);
        else if (e.key === 'Escape') handleCancelEdit(null);
    };
    const handleCloseSnackbar = (event, reason) => {
        if (reason === 'clickaway') return;
        setSnackbar({ ...snackbar, open: false });
    };

    const displayedItems = showAll ? items : items.slice(0, MAX_INITIAL_ITEMS);

    return (
        <>
            <ListItemButton onClick={handleClick} sx={{ py: 1.5 }}>
                <ListItemIcon sx={{ minWidth: 40 }}>{icon}</ListItemIcon>
                <ListItemText primary={<Typography variant="subtitle1" fontWeight="medium">{title}</Typography>} />
                {open ? <ExpandLess /> : <ExpandMore />}
            </ListItemButton>
            <Collapse in={open} timeout="auto" unmountOnExit>
                <List component="div" disablePadding sx={{ pl: 2 }}>
                    {loading && <ListItemText primary={<CircularProgress size={20} sx={{ ml: 2, my: 1 }} />} />}
                    {error && <ListItemText primary={error} sx={{ color: 'error.main', ml: 2 }} />}
                    {!loading && !error && displayedItems.map((item) => (
                        <ListItemButton
                            key={item.id}
                            selected={!editingItemId && selectedItemName === item.name && filterType === title.toLowerCase().slice(0, -1)}
                            onClick={editingItemId === item.id && isAdmin ? (e) => e.stopPropagation() : () => onSelectItem(filterType, item.name)}
                            sx={{ pl: 4, py: 0.5, display: 'flex', alignItems: 'center' }}
                        >
                            {editingItemId === item.id && isAdmin ? (
                                <>
                                    <TextField inputRef={editInputRef} value={editValue} onChange={handleEditInputChange} onKeyDown={(e) => handleEditInputKeyDown(e, item.id)} onBlur={() => handleSaveEdit(null, item.id)} variant="standard" size="small" fullWidth onClick={(e) => e.stopPropagation()} sx={{ mr: 1 }} />
                                    <Tooltip title="Save">
                                        <IconButton size="small" onClick={(e) => handleSaveEdit(e, item.id)} color="primary" aria-label="save"><SaveIcon fontSize="inherit" /></IconButton>
                                    </Tooltip>
                                    <Tooltip title="Cancel">
                                        <IconButton size="small" onClick={handleCancelEdit} aria-label="cancel"><CancelIcon fontSize="inherit" /></IconButton>
                                    </Tooltip>
                                </>
                            ) : (
                                <>
                                    <ListItemIcon sx={{ minWidth: 30 }}><LabelImportantIcon fontSize="small" /></ListItemIcon>
                                    <ListItemText primary={item.name} primaryTypographyProps={{ variant: 'body2', sx: { flexGrow: 1 } }} />
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
                            <ListItemText primary={showAll ? 'Show Less' : `Show More (${items.length - MAX_INITIAL_ITEMS})`} primaryTypographyProps={{ variant: 'caption', color: 'primary' }} />
                        </ListItemButton>
                    )}
                    {!loading && !error && items.length === 0 && <ListItemText primary="None found" sx={{ pl: 4, fontStyle: 'italic' }} />}
                </List>
            </Collapse>
            <Snackbar open={snackbar.open} autoHideDuration={4000} onClose={handleCloseSnackbar} anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
                <Alert onClose={handleCloseSnackbar} severity={snackbar.severity} sx={{ width: '100%' }}>
                    {snackbar.message}
                </Alert>
            </Snackbar>
        </>
    );
}

function FilterSidebar({ onFilterChange, activeFilter, onItemUpdatedOrDeletedInApp }) {
    return (
        <Box
            sx={{
                width: '100%',
                maxWidth: { xs: '100%', sm: 280, md: 300 }, // Responsive width
                bgcolor: 'background.paper',
                borderRight: { xs: 'none', sm: '1px solid' },
                borderColor: 'divider',
                height: { xs: 'auto', sm: 'calc(100vh - 64px)' }, // Adjust height for mobile
                overflowY: 'auto',
                position: { xs: 'relative', sm: 'sticky' }, // Sticky only on larger screens
                top: { sm: '64px' }, // AppBar height
                zIndex: {sm: (theme) => theme.zIndex.drawer }, // Ensure it's below AppBar on mobile if AppBar is fixed
            }}
        >
            <List component="nav" aria-labelledby="nested-list-subheader" sx={{pt:0}}>
                <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <Typography variant="h6" fontWeight="medium">Filter Builds</Typography>
                    {/* Potentially an icon button to toggle sidebar on mobile if it becomes a drawer */}
                </Box>
                <Divider />
                <ListItemButton onClick={() => onFilterChange(null, null)} selected={!activeFilter || (!activeFilter.type && !activeFilter.name)} sx={{ py: 1.5 }}>
                    <ListItemIcon sx={{ minWidth: 40 }}><HomeIcon /></ListItemIcon>
                    <ListItemText primary={<Typography variant="subtitle1" fontWeight="medium">All Builds</Typography>} />
                </ListItemButton>
                <Divider />

                <FilterableListSection title="Authors" icon={<PeopleIcon />} fetchItems={getAllAuthors} onSelectItem={onFilterChange} selectedItemName={activeFilter?.type === 'author' ? activeFilter.name : null} filterType="author" onItemUpdatedOrDeleted={onItemUpdatedOrDeletedInApp} />
                <Divider />
                <FilterableListSection title="Themes" icon={<CategoryIcon />} fetchItems={getAllThemes} onSelectItem={onFilterChange} selectedItemName={activeFilter?.type === 'theme' ? activeFilter.name : null} filterType="theme" onItemUpdatedOrDeleted={onItemUpdatedOrDeletedInApp} />
                <Divider />
                <FilterableListSection title="Colors" icon={<PaletteIcon />} fetchItems={getAllColors} onSelectItem={onFilterChange} selectedItemName={activeFilter?.type === 'color' ? activeFilter.name : null} filterType="color" onItemUpdatedOrDeleted={onItemUpdatedOrDeletedInApp} />
            </List>
        </Box>
    );
}

export default FilterSidebar;