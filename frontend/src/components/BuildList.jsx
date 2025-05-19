// File: frontend/src/components/BuildList.jsx
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { deleteBuild as apiDeleteBuild } from '../api/buildService';
import { useAuth } from '../context/AuthContext.jsx';

import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardActions from '@mui/material/CardActions';
import CardMedia from '@mui/material/CardMedia';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import Chip from '@mui/material/Chip';
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Alert from '@mui/material/Alert';
import Tooltip from '@mui/material/Tooltip';

import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import DownloadIcon from '@mui/icons-material/Download';
import CategoryIcon from '@mui/icons-material/Category';
import PaletteIcon from '@mui/icons-material/Palette';
import PeopleIcon from '@mui/icons-material/People';
import ImageIcon from '@mui/icons-material/Image';

// Define fixed dimensions for the cards
const CARD_WIDTH = '345px';
const CARD_HEIGHT = '450px';
const CARD_MEDIA_HEIGHT = 160;

// Placeholder colors (can be theme-based or fixed)
const PLACEHOLDER_BACKGROUND_COLOR = 'grey.200'; // MUI theme key for grey[200]
const PLACEHOLDER_ICON_COLOR = 'grey.400';     // MUI theme key for grey[400]

const fetchSchematicFile = async (buildIdentifier) => {
    const response = await fetch(`/api/builds/${buildIdentifier}/schem`);
    if (!response.ok) {
        let errorMsg = `Failed to download schematic for build ${buildIdentifier}. `
            + `Status: ${response.status}`;
        try {
            const errorData = await response.json();
            errorMsg = errorData.detail || errorData.message || errorMsg;
        } catch (e) {
            // If parsing errorData fails, use the original error message with statusText
            errorMsg = `${errorMsg} - ${response.statusText}`;
        }
        throw new Error(errorMsg);
    }
    return response.blob();
};

/**
 * Renders a list of Minecraft builds, each in a Card component.
 * Cards will have a fixed width and height for uniform appearance.
 * @param {object} props - The component's props.
 * @param {Array<object>} props.builds - The list of builds to render.
 * @param {Function} props.onBuildDeleted - Callback when a build is deleted.
 * @param {Function} props.onEditBuild - Callback to edit a build.
 * @returns {JSX.Element} The rendered list of builds.
 */
function BuildList({ builds, onBuildDeleted, onEditBuild }) {
    const { hasRole, isAuthenticated } = useAuth();
    const isAdmin = isAuthenticated && hasRole('ROLE_ADMIN');
    const [imageErrors, setImageErrors] = useState({});

    const handleDelete = async (buildId, buildName) => {
        if (!isAdmin) {
            alert("You don't have permission to delete builds.");
            return;
        }
        if (window.confirm(`Are you sure you want to delete the build "${buildName}"?`)) {
            try {
                await apiDeleteBuild(buildId);
                if (onBuildDeleted) {
                    onBuildDeleted(buildId);
                }
            } catch (err) {
                alert(`Failed to delete build: ${err.message}`);
                console.error("Delete build error:", err);
            }
        }
    };

    const handleDownloadSchematic = async (buildId, buildName) => {
        try {
            const blob = await fetchSchematicFile(buildId);
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.style.display = 'none';
            a.href = url;
            // Sanitize buildName for use in filename
            a.download = `${buildName.replace(/[^a-z0-9_.-]/gi, '_')}.schem`;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            a.remove();
        } catch (error) {
            console.error('Download error:', error);
            alert(`Could not download schematic: ${error.message}`);
        }
    };

    const handleImageError = (buildId) => {
        setImageErrors((prevErrors) => ({
            ...prevErrors,
            [buildId]: true,
        }));
    };

    const renderChips = (items, icon) => {
        if (!items || items.length === 0) {
            return (
                <Typography
                    variant="body2"
                    color="text.secondary"
                    component="span"
                    sx={{ ml: icon ? 0 : 1 }} // No margin if icon is present in Chip
                >
                    N/A
                </Typography>
            );
        }
        return items.map((item) => (
            <Chip
                key={item.id || item.name} // Ensure unique key
                icon={icon}
                label={item.name}
                size="small"
                sx={{ mr: 0.5, mb: 0.5 }}
            />
        ));
    };

    if (!builds || builds.length === 0) {
        return (
            <Alert severity="info" sx={{ mt: 3 }}>
                No builds found. Try adjusting filters or adding new builds.
            </Alert>
        );
    }

    return (
        <Grid container spacing={{ xs: 2, md: 3 }} sx={{ mt: 0 }}>
            {builds.map((build) => {
                const firstScreenshotUrl = build.screenshots && build.screenshots.length > 0
                    ? build.screenshots[0]
                    : null;
                const hasImageLoadingError = imageErrors[build.id];
                const showPlaceholder = !firstScreenshotUrl || hasImageLoadingError;

                return (
                    <Grid item xs={12} sm={6} md={4} key={build.id}>
                        <Card sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            width: CARD_WIDTH,
                            height: CARD_HEIGHT,
                            boxShadow: 3,
                            margin: 'auto' // Center card in grid item if grid item is wider
                        }}>
                            {showPlaceholder ? (
                                <Box sx={{
                                    height: CARD_MEDIA_HEIGHT,
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    backgroundColor: PLACEHOLDER_BACKGROUND_COLOR,
                                    color: PLACEHOLDER_ICON_COLOR,
                                }}>
                                    <ImageIcon fontSize="large" />
                                </Box>
                            ) : (
                                <CardMedia
                                    component="img"
                                    sx={{
                                        height: CARD_MEDIA_HEIGHT,
                                        objectFit: 'cover'
                                    }}
                                    image={firstScreenshotUrl}
                                    alt={`Screenshot of ${build.name}`}
                                    onError={() => handleImageError(build.id)}
                                />
                            )}
                            <CardContent sx={{
                                flexGrow: 1,
                                pb: 1, // padding-bottom
                                overflow: 'hidden' // Prevent content from overflowing card
                            }}>
                                <Tooltip title={build.name} placement="top">
                                    <Typography
                                        gutterBottom
                                        variant="h5"
                                        component="div"
                                        noWrap // Prevents text from wrapping to a new line
                                    >
                                        {build.name}
                                    </Typography>
                                </Tooltip>
                                <Tooltip
                                    title={build.description || 'No description available.'}
                                    placement="top"
                                >
                                    <Typography
                                        variant="body2"
                                        color="text.secondary"
                                        gutterBottom
                                        sx={{
                                            height: '3.6em', // Approx 2 lines (1.2em per line * 3)
                                            lineHeight: '1.2em', // Adjust based on font
                                            overflow: 'hidden',
                                            textOverflow: 'ellipsis',
                                            display: '-webkit-box',
                                            WebkitLineClamp: 2, // Show 2 lines
                                            WebkitBoxOrient: 'vertical',
                                        }}
                                    >
                                        {build.description || 'No description available.'}
                                    </Typography>
                                </Tooltip>
                                <Box sx={{ mb: 0.5, display: 'flex', alignItems: 'center' }}>
                                    <PeopleIcon fontSize="small" sx={{ mr: 0.5 }} />
                                    <Typography variant="subtitle2" component="strong" sx={{ mr: 0.5 }}>
                                        Authors:
                                    </Typography>
                                    {renderChips(build.authors)}
                                </Box>
                                <Box sx={{ mb: 0.5, display: 'flex', alignItems: 'center' }}>
                                    <CategoryIcon fontSize="small" sx={{ mr: 0.5 }} />
                                    <Typography variant="subtitle2" component="strong" sx={{ mr: 0.5 }}>
                                        Themes:
                                    </Typography>
                                    {renderChips(build.themes)}
                                </Box>
                                <Box sx={{ display: 'flex', alignItems: 'center' }}>
                                    <PaletteIcon fontSize="small" sx={{ mr: 0.5 }} />
                                    <Typography variant="subtitle2" component="strong" sx={{ mr: 0.5 }}>
                                        Colors:
                                    </Typography>
                                    {renderChips(build.colors)}
                                </Box>
                            </CardContent>
                            <CardActions sx={{
                                justifyContent: 'flex-end',
                                pt: 0, // Remove default top padding
                                pb: 1, // Add some padding at the bottom
                                pr: 1  // Add some padding at the right
                            }}>
                                <Tooltip title="Download Schematic">
                                    <IconButton
                                        aria-label="download schematic"
                                        onClick={() =>
                                            handleDownloadSchematic(build.id, build.name)}
                                        color="secondary"
                                    >
                                        <DownloadIcon />
                                    </IconButton>
                                </Tooltip>
                                {isAdmin && (
                                    <>
                                        <Tooltip title="Edit Build">
                                            <IconButton
                                                aria-label="edit build"
                                                onClick={() => onEditBuild(build)}
                                                color="primary"
                                            >
                                                <EditIcon />
                                            </IconButton>
                                        </Tooltip>
                                        <Tooltip title="Delete Build">
                                            <IconButton
                                                aria-label="delete build"
                                                onClick={() =>
                                                    handleDelete(build.id, build.name)}
                                                color="error"
                                            >
                                                <DeleteIcon />
                                            </IconButton>
                                        </Tooltip>
                                    </>
                                )}
                            </CardActions>
                        </Card>
                    </Grid>
                );
            })}
        </Grid>
    );
}

BuildList.propTypes = {
    builds: PropTypes.arrayOf(PropTypes.shape({
        id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
        name: PropTypes.string.isRequired,
        description: PropTypes.string,
        authors: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
            name: PropTypes.string,
        })),
        themes: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
            name: PropTypes.string,
        })),
        colors: PropTypes.arrayOf(PropTypes.shape({
            id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
            name: PropTypes.string,
        })),
        screenshots: PropTypes.arrayOf(PropTypes.string),
    })).isRequired,
    onBuildDeleted: PropTypes.func.isRequired,
    onEditBuild: PropTypes.func.isRequired,
};

export default BuildList;