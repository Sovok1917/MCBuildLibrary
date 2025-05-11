// File: frontend/src/components/FilterSidebar.jsx
import React, { useState, useEffect } from 'react';
import { getAllAuthors } from '../api/authorService';
import { getAllThemes } from '../api/themeService';
import { getAllColors } from '../api/colorService';

import Box from '@mui/material/Box';
import List from '@mui/material/List';
import ListItemButton from '@mui/material/ListItemButton';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Collapse from '@mui/material/Collapse';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import Divider from '@mui/material/Divider';

// Icons
import PeopleIcon from '@mui/icons-material/People';
import CategoryIcon from '@mui/icons-material/Category';
import PaletteIcon from '@mui/icons-material/Palette';
import ExpandLess from '@mui/icons-material/ExpandLess';
import ExpandMore from '@mui/icons-material/ExpandMore';
import LabelImportantIcon from '@mui/icons-material/LabelImportant'; // For individual items
import HomeIcon from '@mui/icons-material/Home'; // For "All Builds"

const MAX_INITIAL_ITEMS = 5; // Show this many items before "Show More"

function FilterableListSection({ title, icon, fetchItems, onSelectItem, selectedItemName, filterType }) {
    const [open, setOpen] = useState(false);
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [showAll, setShowAll] = useState(false);

    useEffect(() => {
        const loadItems = async () => {
            try {
                setLoading(true);
                const data = await fetchItems();
                // DTOs have { id, name, relatedBuilds }
                // We only need id and name for the list
                setItems(data.map(item => ({ id: item.id, name: item.name })).sort((a, b) => a.name.localeCompare(b.name)));
                setError(null);
            } catch (err) {
                setError(`Failed to load ${title.toLowerCase()}`);
                console.error(err);
            } finally {
                setLoading(false);
            }
        };
        loadItems();
    }, [fetchItems, title]);

    const handleClick = () => setOpen(!open);
    const handleToggleShowAll = (e) => {
        e.stopPropagation(); // Prevent list item click when clicking "Show More/Less"
        setShowAll(!showAll);
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
                    {loading && <ListItemText primary={<CircularProgress size={20} sx={{ml:2, my:1}} />} />}
                    {error && <ListItemText primary={error} sx={{ color: 'error.main', ml:2 }} />}
                    {!loading && !error && displayedItems.map((item) => (
                        <ListItemButton
                            key={item.id}
                            selected={selectedItemName === item.name && filterType === title.toLowerCase().slice(0, -1)} // Check filterType too
                            onClick={() => onSelectItem(title.toLowerCase().slice(0, -1), item.name)} // Pass type and name
                            sx={{ pl: 4, py: 0.5 }}
                        >
                            <ListItemIcon sx={{ minWidth: 30 }}><LabelImportantIcon fontSize="small" /></ListItemIcon>
                            <ListItemText primary={item.name} primaryTypographyProps={{ variant: 'body2' }} />
                        </ListItemButton>
                    ))}
                    {!loading && !error && items.length > MAX_INITIAL_ITEMS && (
                        <ListItemButton onClick={handleToggleShowAll} sx={{ pl: 4, py: 0.5 }}>
                            <ListItemText
                                primary={showAll ? 'Show Less' : `Show More (${items.length - MAX_INITIAL_ITEMS})`}
                                primaryTypographyProps={{ variant: 'caption', color: 'primary' }}
                            />
                        </ListItemButton>
                    )}
                    {!loading && !error && items.length === 0 && (
                        <ListItemText primary="None found" sx={{ pl: 4, fontStyle: 'italic' }} />
                    )}
                </List>
            </Collapse>
        </>
    );
}

/**
 * Sidebar component for filtering builds by authors, themes, and colors.
 * @param {Object} props - Component props.
 * @param {Function} props.onFilterChange - Callback when a filter is selected.
 *                                          Receives an object like { type: 'author', name: 'John Doe' }
 *                                          or { type: null, name: null } for clearing filter.
 * @param {Object} props.activeFilter - The currently active filter e.g. {type: 'author', name: 'John Doe'}
 */
function FilterSidebar({ onFilterChange, activeFilter }) {
    return (
        <Box
            sx={{
                width: '100%',
                maxWidth: 300, // Adjust as needed
                bgcolor: 'background.paper',
                borderRight: '1px solid',
                borderColor: 'divider',
                height: 'calc(100vh - 64px)', // Full height minus AppBar height (approx)
                overflowY: 'auto',
                position: 'sticky', // Make sidebar sticky
                top: '64px', // Align with bottom of AppBar
            }}
        >
            <List component="nav" aria-labelledby="nested-list-subheader">
                <Typography variant="h6" sx={{ p: 2, fontWeight: 'bold' }}>
                    Filter Builds
                </Typography>
                <Divider />
                <ListItemButton
                    onClick={() => onFilterChange(null, null)} // Clear filter
                    selected={!activeFilter || (!activeFilter.type && !activeFilter.name)}
                    sx={{ py: 1.5 }}
                >
                    <ListItemIcon sx={{ minWidth: 40 }}><HomeIcon /></ListItemIcon>
                    <ListItemText primary={<Typography variant="subtitle1" fontWeight="medium">All Builds</Typography>} />
                </ListItemButton>
                <Divider />

                <FilterableListSection
                    title="Authors"
                    icon={<PeopleIcon />}
                    fetchItems={getAllAuthors}
                    onSelectItem={onFilterChange}
                    selectedItemName={activeFilter?.type === 'author' ? activeFilter.name : null}
                    filterType="author"
                />
                <Divider />
                <FilterableListSection
                    title="Themes"
                    icon={<CategoryIcon />}
                    fetchItems={getAllThemes}
                    onSelectItem={onFilterChange}
                    selectedItemName={activeFilter?.type === 'theme' ? activeFilter.name : null}
                    filterType="theme"
                />
                <Divider />
                <FilterableListSection
                    title="Colors"
                    icon={<PaletteIcon />}
                    fetchItems={getAllColors}
                    onSelectItem={onFilterChange}
                    selectedItemName={activeFilter?.type === 'color' ? activeFilter.name : null}
                    filterType="color"
                />
            </List>
        </Box>
    );
}

export default FilterSidebar;