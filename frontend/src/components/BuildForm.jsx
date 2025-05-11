// File: frontend/src/components/BuildForm.jsx
import React, { useState, useEffect } from 'react';
import { createBuild, updateBuild } from '../api/buildService';

import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import CircularProgress from '@mui/material/CircularProgress';
import Alert from '@mui/material/Alert';

const AUTHORS_PARAM_NAME = "authors";
const THEMES_PARAM_NAME = "themes";
const COLORS_PARAM_NAME = "colors";
const NAME_PARAM_NAME = "name";
const DESCRIPTION_PARAM_NAME = "description";
const SCHEM_FILE_PARAM_NAME = "schemFile";
const SCREENSHOTS_PARAM_NAME = "screenshots";

const INITIAL_FORM_STATE = {
    name: '',
    authorNames: '',
    themeNames: '',
    description: '',
    colorNames: '',
    screenshotUrls: '',
    schemFile: null,
};

function BuildForm({ onBuildCreated, existingBuild, onBuildUpdated, onCancelForm }) {
    const [formData, setFormData] = useState(INITIAL_FORM_STATE);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState('');
    const [selectedFileName, setSelectedFileName] = useState('');

    // isEditMode is true only if existingBuild has an ID (meaning it's an actual existing record)
    const isEditMode = Boolean(existingBuild && existingBuild.id);

    useEffect(() => {
        if (existingBuild) { // If any existingBuild object is passed (for edit or pre-fill create)
            setFormData({
                name: existingBuild.name || '',
                // For pre-fill create, existingBuild.authors might not exist, so check existingBuild.authorNames
                authorNames: (existingBuild.authors ? existingBuild.authors.map(a => a.name).join(', ') : existingBuild.authorNames) || '',
                themeNames: (existingBuild.themes ? existingBuild.themes.map(t => t.name).join(', ') : existingBuild.themeNames) || '',
                description: existingBuild.description || '',
                colorNames: (existingBuild.colors ? existingBuild.colors.map(c => c.name).join(', ') : existingBuild.colorNames) || '',
                screenshotUrls: (existingBuild.screenshots ? existingBuild.screenshots.join(', ') : existingBuild.screenshotUrls) || '',
                schemFile: null, // Always reset file input
            });
        } else { // No existingBuild, so it's a fresh create form
            setFormData(INITIAL_FORM_STATE);
        }
        setSelectedFileName('');
        setSuccessMessage('');
        setError(null);
    }, [existingBuild]); // Depend on the whole existingBuild object

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData({ ...formData, [name]: value });
    };

    const handleFileChange = (e) => {
        const file = e.target.files[0];
        if (file) {
            setFormData({ ...formData, schemFile: file });
            setSelectedFileName(file.name);
        } else {
            setFormData({ ...formData, schemFile: null });
            setSelectedFileName('');
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setIsSubmitting(true);
        setError(null);
        setSuccessMessage('');

        const data = new FormData();
        data.append(NAME_PARAM_NAME, formData.name);
        formData.authorNames.split(',').map(s => s.trim()).filter(s => s)
            .forEach(author => data.append(AUTHORS_PARAM_NAME, author));
        formData.themeNames.split(',').map(s => s.trim()).filter(s => s)
            .forEach(theme => data.append(THEMES_PARAM_NAME, theme));
        formData.colorNames.split(',').map(s => s.trim()).filter(s => s)
            .forEach(color => data.append(COLORS_PARAM_NAME, color));

        if (formData.description) data.append(DESCRIPTION_PARAM_NAME, formData.description);
        if (formData.screenshotUrls) {
            formData.screenshotUrls.split(',').map(url => url.trim()).filter(url => url)
                .forEach(url => data.append(SCREENSHOTS_PARAM_NAME, url));
        }

        if (formData.schemFile) {
            data.append(SCHEM_FILE_PARAM_NAME, formData.schemFile);
        } else if (!isEditMode) { // File required only for true "create" mode (not pre-filled create)
            // and not for edit mode.
            if (!existingBuild || !existingBuild.id) { // If it's not a true edit, and no file, then error
                setError("Schematic file is required for new builds.");
                setIsSubmitting(false);
                return;
            }
        }

        try {
            if (isEditMode) { // True edit mode (existingBuild has an ID)
                const updated = await updateBuild(existingBuild.id, data);
                setSuccessMessage(`Build "${updated.name}" updated successfully!`);
                if (onBuildUpdated) onBuildUpdated(updated);
            } else { // Create mode (could be fresh or pre-filled from filter)
                const newBuild = await createBuild(data);
                setSuccessMessage(`Build "${newBuild.name}" created successfully!`);
                setFormData(INITIAL_FORM_STATE);
                setSelectedFileName('');
                if (onBuildCreated) onBuildCreated(newBuild);
            }
        } catch (err) {
            setError(err.message || `Failed to ${isEditMode ? 'update' : 'create'} build.`);
            console.error("Form submit error:", err);
        } finally {
            setIsSubmitting(false);
        }
    };

    // Determine title based on whether it's a true edit or a create (possibly pre-filled)
    const formTitle = isEditMode ? `Edit Build: ${existingBuild.name}` : 'Create New Build';

    return (
        <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', p: { xs: 2, sm: 3 }, bgcolor: 'background.paper' }}>
            <Typography component="h2" variant="h5" gutterBottom>
                {formTitle}
            </Typography>

            {error && <Alert severity="error" sx={{ width: '100%', mb: 2 }}>{error}</Alert>}
            {successMessage && !error && <Alert severity="success" sx={{ width: '100%', mb: 2 }}>{successMessage}</Alert>}

            <Box component="form" onSubmit={handleSubmit} sx={{ mt: 1, width: '100%' }}>
                <TextField margin="normal" required fullWidth id="name" label="Build Name" name="name" value={formData.name} onChange={handleChange} autoFocus />
                <TextField margin="normal" required fullWidth id="authorNames" label="Authors (comma-separated)" name="authorNames" value={formData.authorNames} onChange={handleChange} />
                <TextField margin="normal" required fullWidth id="themeNames" label="Themes (comma-separated)" name="themeNames" value={formData.themeNames} onChange={handleChange} />
                <TextField margin="normal" required fullWidth id="colorNames" label="Colors (comma-separated)" name="colorNames" value={formData.colorNames} onChange={handleChange} />
                <TextField margin="normal" fullWidth id="description" label="Description" name="description" multiline rows={3} value={formData.description} onChange={handleChange} />
                <TextField margin="normal" fullWidth id="screenshotUrls" label="Screenshot URLs (comma-separated)" name="screenshotUrls" value={formData.screenshotUrls} onChange={handleChange} helperText="Enter direct image URLs, separated by commas." />
                <Button variant="outlined" component="label" fullWidth sx={{ mt: 2, mb: 1 }}>
                    {selectedFileName || `Upload Schematic File (.schem) ${(!isEditMode && (!existingBuild || !existingBuild.id)) ? '*' : '(Optional)'}`}
                    <input type="file" hidden accept=".schem" onChange={handleFileChange} />
                </Button>

                <Box sx={{ display: 'flex', gap: 2, mt: 3, mb: 2 }}>
                    <Button type="submit" fullWidth variant="contained" disabled={isSubmitting} startIcon={isSubmitting ? <CircularProgress size={20} color="inherit" /> : null}>
                        {isSubmitting ? 'Submitting...' : (isEditMode ? 'Update Build' : 'Create Build')}
                    </Button>
                    <Button type="button" fullWidth variant="outlined" onClick={onCancelForm} disabled={isSubmitting}>
                        {isEditMode ? 'Cancel Edit' : 'Cancel Create'}
                    </Button>
                </Box>
            </Box>
        </Box>
    );
}

export default BuildForm;