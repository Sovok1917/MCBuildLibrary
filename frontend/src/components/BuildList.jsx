// File: frontend/src/components/BuildList.jsx
import React from 'react';
import { deleteBuild as apiDeleteBuild } from '../api/buildService';

import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import CardActions from '@mui/material/CardActions';
import CardMedia from '@mui/material/CardMedia';
import Typography from '@mui/material/Typography';
import IconButton from '@mui/material/IconButton';
import Chip from '@mui/material/Chip'; // Chip is used here, ensure it's imported
import Box from '@mui/material/Box';
import Grid from '@mui/material/Grid';
import Alert from '@mui/material/Alert';

// Icons
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import CategoryIcon from '@mui/icons-material/Category';
import PaletteIcon from '@mui/icons-material/Palette';
import PeopleIcon from '@mui/icons-material/People';
import ImageIcon from '@mui/icons-material/Image';

function BuildList({ builds, onBuildDeleted, onEditBuild }) {
    const handleDelete = async (buildId, buildName) => {
        if (window.confirm(`Are you sure you want to delete the build "${buildName}"?`)) {
            try {
                await apiDeleteBuild(buildId);
                alert(`Build "${buildName}" deleted successfully.`);
                if (onBuildDeleted) {
                    onBuildDeleted();
                }
            } catch (err) {
                alert(`Failed to delete build: ${err.message}`);
                console.error("Delete build error:", err);
            }
        }
    };

    const renderChips = (items, icon) => {
        if (!items || items.length === 0) {
            return <Typography variant="body2" color="text.secondary" component="span">N/A</Typography>;
        }
        return items.map((item) => (
            <Chip
                key={item.id || item.name}
                icon={icon}
                label={item.name}
                size="small"
                sx={{ mr: 0.5, mb: 0.5 }}
            />
        ));
    };

    if (!builds || builds.length === 0) {
        return <Alert severity="info" sx={{ mt: 3 }}>No builds found.</Alert>;
    }

    return (
        <Grid container spacing={3} sx={{ mt: 2 }}>
            {builds.map((build) => {
                const firstScreenshotUrl = build.screenshots && build.screenshots.length > 0
                    ? build.screenshots[0]
                    : null;

                return (
                    // Removed 'item' prop. xs, sm, md are applied directly to the Grid acting as an item.
                    <Grid xs={12} sm={6} md={4} key={build.id}>
                        <Card sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
                            {firstScreenshotUrl ? (
                                <CardMedia
                                    component="img"
                                    sx={{ height: 140, objectFit: 'cover' }}
                                    image={firstScreenshotUrl}
                                    alt={`Screenshot of ${build.name}`}
                                    onError={(e) => {
                                        e.target.onerror = null;
                                        e.target.style.display = 'none';
                                    }}
                                />
                            ) : (
                                <Box sx={{ height: 140, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'grey.200', color: 'grey.500' }}>
                                    <ImageIcon fontSize="large" />
                                </Box>
                            )}
                            <CardContent sx={{ flexGrow: 1 }}>
                                <Typography gutterBottom variant="h5" component="div">
                                    {build.name}
                                </Typography>
                                <Typography variant="body2" color="text.secondary" paragraph>
                                    {build.description || 'No description available.'}
                                </Typography>
                                <Box sx={{ mb: 1 }}>
                                    <Typography variant="subtitle2" component="strong" sx={{ mr: 1 }}>
                                        <PeopleIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} />
                                        Authors:
                                    </Typography>
                                    {renderChips(build.authors)}
                                </Box>
                                <Box sx={{ mb: 1 }}>
                                    <Typography variant="subtitle2" component="strong" sx={{ mr: 1 }}>
                                        <CategoryIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} />
                                        Themes:
                                    </Typography>
                                    {renderChips(build.themes)}
                                </Box>
                                <Box>
                                    <Typography variant="subtitle2" component="strong" sx={{ mr: 1 }}>
                                        <PaletteIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} />
                                        Colors:
                                    </Typography>
                                    {renderChips(build.colors)}
                                </Box>
                            </CardContent>
                            <CardActions sx={{ justifyContent: 'flex-end', pt: 0, pb: 1, pr: 1 }}>
                                <IconButton aria-label="edit build" onClick={() => onEditBuild(build)} color="primary">
                                    <EditIcon />
                                </IconButton>
                                <IconButton aria-label="delete build" onClick={() => handleDelete(build.id, build.name)} color="error">
                                    <DeleteIcon />
                                </IconButton>
                            </CardActions>
                        </Card>
                    </Grid>
                );
            })}
        </Grid>
    );
}

export default BuildList;