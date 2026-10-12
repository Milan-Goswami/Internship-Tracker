import React, { useState, useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import './OpportunityModal.css';

export const OpportunityModal = ({
  isOpen,
  onClose,
  onSave,
  initialData,
  companies = [],
  onCreateCompany,
}) => {
  const isEdit = Boolean(initialData);

  const [companyName, setCompanyName] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [locationType, setLocationType] = useState('REMOTE');
  const [locationCity, setLocationCity] = useState('');
  const [stipendAmount, setStipendAmount] = useState('');
  const [stipendCurrency, setStipendCurrency] = useState('USD');
  const [stipendPeriod, setStipendPeriod] = useState('HOURLY');
  const [durationMonths, setDurationMonths] = useState(3);
  const [applicationDeadline, setApplicationDeadline] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');

  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (initialData) {
      setCompanyName(initialData.companyName || '');
      setTitle(initialData.title || '');
      setDescription(initialData.description || '');
      setLocationType(initialData.locationType || 'REMOTE');
      setLocationCity(initialData.locationCity || '');
      setStipendAmount(initialData.stipendAmount ? String(initialData.stipendAmount) : '');
      setStipendCurrency(initialData.stipendCurrency || 'USD');
      setStipendPeriod(initialData.stipendPeriod || 'HOURLY');
      setDurationMonths(initialData.durationMonths || 3);
      setApplicationDeadline(
        initialData.applicationDeadline ? initialData.applicationDeadline.substring(0, 10) : ''
      );
      setSourceUrl(initialData.sourceUrl || '');
    } else {
      setCompanyName('');
      setTitle('');
      setDescription('');
      setLocationType('REMOTE');
      setLocationCity('');
      setStipendAmount('');
      setStipendCurrency('USD');
      setStipendPeriod('HOURLY');
      setDurationMonths(3);
      setApplicationDeadline('');
      setSourceUrl('');
    }
    setError(null);
  }, [initialData, isOpen]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!title.trim()) {
      setError('Role title is required.');
      return;
    }
    if (!isEdit && !companyName.trim()) {
      setError('Company name is required.');
      return;
    }

    setIsSubmitting(true);
    setError(null);

    try {
      let companyId = initialData?.companyId;

      if (!isEdit) {
        const existingCompany = companies.find(
          (c) => c.name.toLowerCase() === companyName.trim().toLowerCase()
        );
        if (existingCompany) {
          companyId = existingCompany.id;
        } else if (onCreateCompany) {
          const newCompany = await onCreateCompany({ name: companyName.trim() });
          companyId = newCompany.id;
        }
      }

      const payload = {
        title: title.trim(),
        description: description.trim() || null,
        locationType: locationType || null,
        locationCity: locationCity.trim() || null,
        stipendAmount: stipendAmount ? parseFloat(stipendAmount) : null,
        stipendCurrency: stipendCurrency || 'USD',
        stipendPeriod: stipendPeriod || 'HOURLY',
        durationMonths: durationMonths ? parseInt(durationMonths, 10) : null,
        applicationDeadline: applicationDeadline ? new Date(applicationDeadline).toISOString() : null,
        sourceUrl: sourceUrl.trim() || null,
      };

      if (!isEdit) {
        payload.companyId = companyId;
      }

      await onSave(payload, isEdit, initialData?.id);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to save opportunity');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">
            {isEdit ? 'Edit Custom Opportunity' : 'Create Custom Opportunity'}
          </h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            ✕
          </button>
        </div>

        <form onSubmit={handleSubmit} className="modal-body">
          <div className="opportunity-modal-form">
            {error && (
              <div
                style={{
                  padding: '8px 12px',
                  backgroundColor: 'rgba(239, 68, 68, 0.1)',
                  border: '1px solid rgba(239, 68, 68, 0.2)',
                  borderRadius: 'var(--radius-md)',
                  color: '#ef4444',
                  fontSize: 'var(--font-size-xs)',
                }}
              >
                {error}
              </div>
            )}

            {!isEdit && (
              <div className="opportunity-form-group full-width">
                <label className="opportunity-form-label">Company Name *</label>
                <input
                  type="text"
                  className="opportunity-form-input"
                  placeholder="e.g. Figma, Stripe, OpenAI"
                  value={companyName}
                  onChange={(e) => setCompanyName(e.target.value)}
                  list="known-companies-list"
                  required
                />
                <datalist id="known-companies-list">
                  {companies.map((c) => (
                    <option key={c.id} value={c.name} />
                  ))}
                </datalist>
              </div>
            )}

            <div className="opportunity-form-group full-width">
              <label className="opportunity-form-label">Role Title *</label>
              <input
                type="text"
                className="opportunity-form-input"
                placeholder="e.g. Software Engineering Intern (Summer 2026)"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                maxLength={200}
              />
            </div>

            <div className="opportunity-form-row">
              <div className="opportunity-form-group">
                <label className="opportunity-form-label">Work Mode</label>
                <select
                  className="opportunity-form-select"
                  value={locationType}
                  onChange={(e) => setLocationType(e.target.value)}
                >
                  <option value="REMOTE">Remote</option>
                  <option value="HYBRID">Hybrid</option>
                  <option value="ONSITE">On-site</option>
                </select>
              </div>

              <div className="opportunity-form-group">
                <label className="opportunity-form-label">City / Region</label>
                <input
                  type="text"
                  className="opportunity-form-input"
                  placeholder="e.g. San Francisco, CA"
                  value={locationCity}
                  onChange={(e) => setLocationCity(e.target.value)}
                  maxLength={150}
                />
              </div>
            </div>

            <div className="opportunity-form-row">
              <div className="opportunity-form-group">
                <label className="opportunity-form-label">Stipend / Salary</label>
                <input
                  type="number"
                  className="opportunity-form-input"
                  placeholder="e.g. 55"
                  step="0.01"
                  min="0"
                  value={stipendAmount}
                  onChange={(e) => setStipendAmount(e.target.value)}
                />
              </div>

              <div className="opportunity-form-group">
                <label className="opportunity-form-label">Period</label>
                <select
                  className="opportunity-form-select"
                  value={stipendPeriod}
                  onChange={(e) => setStipendPeriod(e.target.value)}
                >
                  <option value="HOURLY">Hourly ($/hr)</option>
                  <option value="MONTHLY">Monthly ($/mo)</option>
                  <option value="TOTAL">Total Stipend</option>
                </select>
              </div>
            </div>

            <div className="opportunity-form-row">
              <div className="opportunity-form-group">
                <label className="opportunity-form-label">Duration (Months)</label>
                <input
                  type="number"
                  className="opportunity-form-input"
                  min="1"
                  max="24"
                  value={durationMonths}
                  onChange={(e) => setDurationMonths(e.target.value)}
                />
              </div>

              <div className="opportunity-form-group">
                <label className="opportunity-form-label">Application Deadline</label>
                <input
                  type="date"
                  className="opportunity-form-input"
                  value={applicationDeadline}
                  onChange={(e) => setApplicationDeadline(e.target.value)}
                />
              </div>
            </div>

            <div className="opportunity-form-group full-width">
              <label className="opportunity-form-label">Job Posting URL</label>
              <input
                type="url"
                className="opportunity-form-input"
                placeholder="https://jobs.example.com/posting/123"
                value={sourceUrl}
                onChange={(e) => setSourceUrl(e.target.value)}
                maxLength={1000}
              />
            </div>

            <div className="opportunity-form-group full-width">
              <label className="opportunity-form-label">Role Description</label>
              <textarea
                className="opportunity-form-textarea"
                placeholder="Paste role description or key qualifications..."
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                maxLength={10000}
              />
            </div>

            <div className="opportunity-modal-actions">
              <Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>
                Cancel
              </Button>
              <Button type="submit" variant="primary" loading={isSubmitting}>
                {isEdit ? 'Save Changes' : 'Create Opportunity'}
              </Button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};
