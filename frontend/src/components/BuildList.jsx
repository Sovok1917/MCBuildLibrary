// File: frontend/src/components/BuildList.jsx
import React from 'react';
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

const fetchSchematicFile = async (buildIdentifier) => {
    const response = await fetch(`/api/builds/${buildIdentifier}/schem`);
    if (!response.ok) {
        let errorMsg = `Failed to download schematic for build ${buildIdentifier}. Status: ${response.status}`;
        try {
            const errorData = await response.json();
            errorMsg = errorData.detail || errorData.message || errorMsg;
        } catch (e) {
            errorMsg = `${errorMsg} - ${response.statusText}`;
        }
        throw new Error(errorMsg);
    }
    return response.blob();
};

function BuildList({ builds, onBuildDeleted, onEditBuild }) {
    const { hasRole, isAuthenticated } = useAuth(); // Get auth state
    const isAdmin = isAuthenticated && hasRole('ROLE_ADMIN');

    const handleDelete = async (buildId, buildName) => {
        if (!isAdmin) {
            alert("You don't have permission to delete builds.");
            return;
        }
        if (window.confirm(`Are you sure you want to delete the build "${buildName}"?`)) {
            try {
                await apiDeleteBuild(buildId);
                // alert(`Build "${buildName}" deleted successfully.`); // Snackbar might be better
                if (onBuildDeleted) {
                    onBuildDeleted(buildId); // Pass ID for potential state update
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
            const filename = `${buildName.replace(/[^a-z0-9_.-]/gi, '_')}.schem`;
            a.download = filename;
            document.body.appendChild(a);
            a.click();
            window.URL.revokeObjectURL(url);
            a.remove();
        } catch (error) {
            console.error('Download error:', error);
            alert(`Could not download schematic: ${error.message}`);
        }
    };

    const renderChips = (items, icon) => {
        if (!items || items.length === 0) {
            return <Typography variant="body2" color="text.secondary" component="span" sx={{ml: icon ? 0 : 1}}>N/A</Typography>;
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
        return <Alert severity="info" sx={{ mt: 3 }}>No builds found. Try adjusting filters or adding new builds.</Alert>;
    }

    return (
        <Grid container spacing={{ xs: 2, md: 3 }} sx={{ mt: 0 }}> {/* Adjusted spacing and mt */}
            {builds.map((build) => {
                const firstScreenshotUrl = build.screenshots && build.screenshots.length > 0
                    ? build.screenshots[0]
                    : null;

                return (
                    <Grid item xs={12} sm={6} md={4} key={build.id}>
                        <Card sx={{ display: 'flex', flexDirection: 'column', height: '100%', boxShadow: 3 }}>
                            {firstScreenshotUrl ? (
                                <CardMedia
                                    component="img"
                                    sx={{ height: 160, objectFit: 'cover' }} // Slightly taller
                                    image={firstScreenshotUrl}
                                    alt={`Screenshot of ${build.name}`}
                                    onError={(e) => { e.target.style.display = 'none'; /* Hide if broken */ }}
                                />
                            ) : (
                                <Box sx={{ height: 160, display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'grey.200', color: 'grey.500' }}>
                                    <ImageIcon fontSize="large" />
                                </Box>
                            )}
                            <CardContent sx={{ flexGrow: 1, pb: 1 }}> {/* Reduced paddingBottom */}
                                <Typography gutterBottom variant="h5" component="div" noWrap title={build.name}>
                                    {build.name}
                                </Typography>
                                <Typography variant="body2" color="text.secondary" paragraph sx={{ maxHeight: '3.6em', overflow: 'hidden', textOverflow: 'ellipsis', display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical' }}>
                                    {build.description || 'No description available.'}
                                </Typography>
                                <Box sx={{ mb: 0.5 }}>
                                    <Typography variant="subtitle2" component="strong" sx={{ mr: 0.5, display: 'inline-flex', alignItems: 'center' }}><PeopleIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} />Authors:</Typography>
                                    {renderChips(build.authors)}
                                </Box>
                                <Box sx={{ mb: 0.5 }}>
                                    <Typography variant="subtitle2" component="strong" sx={{ mr: 0.5, display: 'inline-flex', alignItems: 'center' }}><CategoryIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} />Themes:</Typography>
                                    {renderChips(build.themes)}
                                </Box>
                                <Box>
                                    <Typography variant="subtitle2" component="strong" sx={{ mr: 0.5, display: 'inline-flex', alignItems: 'center' }}><PaletteIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} />Colors:</Typography>
                                    {renderChips(build.colors)}
                                </Box>
                            </CardContent>
                            <CardActions sx={{ justifyContent: 'flex-end', pt: 0, pb: 1, pr: 1 }}>
                                <Tooltip title="Download Schematic">
                                    <IconButton
                                        aria-label="download schematic"
                                        onClick={() => handleDownloadSchematic(build.id, build.name)}
                                        color="secondary"
                                    >
                                        <DownloadIcon />
                                    </IconButton>
                                </Tooltip>
                                {isAdmin && (
                                    <>
                                        <Tooltip title="Edit Build">
                                            <IconButton aria-label="edit build" onClick={() => onEditBuild(build)} color="primary">
                                                <EditIcon />
                                            </IconButton>
                                        </Tooltip>
                                        <Tooltip title="Delete Build">
                                            <IconButton aria-label="delete build" onClick={() => handleDelete(build.id, build.name)} color="error">
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

export default BuildList;