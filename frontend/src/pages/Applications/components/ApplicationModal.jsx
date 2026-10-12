import React, { useState, useEffect } from 'react';
import { Button } from '../../../components/common/Button';
import { Input } from '../../../components/common/Input';
import { opportunityService } from '../../../services/opportunityService';
import './ApplicationModal.css';

const STAGES = [
  { value: 'SAVED', label: 'Saved' },
  { value: 'APPLIED', label: 'Applied' },
  { value: 'SCREENING', label: 'Screening' },
  { value: 'INTERVIEWING', label: 'Interviewing' },
  { value: 'OFFER', label: 'Offer' },
  { value: 'ACCEPTED', label: 'Accepted' },
  { value: 'REJECTED', label: 'Rejected' },
  { value: 'WITHDRAWN', label: 'Withdrawn' },
];

export const ApplicationModal = ({
  isOpen,
  onClose,
  onSubmit,
  initialData = null,
}) => {
  const isEdit = Boolean(initialData);

  const [mode, setMode] = useState('custom'); // 'custom' or 'existing'
  const [existingOpps, setExistingOpps] = useState([]);
  const [selectedOppId, setSelectedOppId] = useState('');

  // Custom Opportunity fields (ADR-03)
  const [companyName, setCompanyName] = useState('');
  const [roleTitle, setRoleTitle] = useState('');
  const [locationType, setLocationType] = useState('REMOTE');
  const [locationCity, setLocationCity] = useState('');
  const [stipendAmount, setStipendAmount] = useState('');

  // Application fields
  const [status, setStatus] = useState('SAVED');
  const [userRating, setUserRating] = useState(0);
  const [personalNotes, setPersonalNotes] = useState('');

  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setError(null);
      if (initialData) {
        setCompanyName(initialData.companyName || '');
        setRoleTitle(initialData.opportunityTitle || '');
        setStatus(initialData.status || 'SAVED');
        setUserRating(initialData.userRating || 0);
        setPersonalNotes(initialData.personalNotes || '');
      } else {
        setCompanyName('');
        setRoleTitle('');
        setLocationType('REMOTE');
        setLocationCity('');
        setStipendAmount('');
        setStatus('SAVED');
        setUserRating(0);
        setPersonalNotes('');
        setSelectedOppId('');

        // Fetch accessible opportunities for existing dropdown
        opportunityService.getOpportunities()
          .then((opps) => setExistingOpps(opps || []))
          .catch(() => setExistingOpps([]));
      }
    }
  }, [isOpen, initialData]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) {
    return null;
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (isEdit) {
      setIsSubmitting(true);
      try {
        await onSubmit({
          id: initialData.id,
          status,
          userRating: userRating > 0 ? userRating : null,
          personalNotes: personalNotes || null,
        });
        onClose();
      } catch (err) {
        setError(err.response?.data?.message || err.message || 'Failed to update application');
      } finally {
        setIsSubmitting(false);
      }
      return;
    }

    // Create flow
    setIsSubmitting(true);
    try {
      let finalOppId = null;

      if (mode === 'existing') {
        if (!selectedOppId) {
          setError('Please select an opportunity from the list');
          setIsSubmitting(false);
          return;
        }
        finalOppId = Number(selectedOppId);
      } else {
        // Create custom opportunity first per ADR-03
        if (!companyName.trim()) {
          setError('Company name is required');
          setIsSubmitting(false);
          return;
        }
        if (!roleTitle.trim()) {
          setError('Role title is required');
          setIsSubmitting(false);
          return;
        }

        // 1. Get or create company
        const companies = await opportunityService.getCompanies(companyName.trim());
        let company = companies.find(
          (c) => c.name.toLowerCase() === companyName.trim().toLowerCase()
        );
        if (!company) {
          company = await opportunityService.createCompany({
            name: companyName.trim(),
          });
        }

        // 2. Create opportunity
        const createdOpp = await opportunityService.createOpportunity({
          companyId: company.id,
          title: roleTitle.trim(),
          locationType,
          locationCity: locationCity.trim() || null,
          stipendAmount: stipendAmount ? parseFloat(stipendAmount) : null,
        });
        finalOppId = createdOpp.id;
      }

      // 3. Create application
      await onSubmit({
        opportunityId: finalOppId,
        status,
        userRating: userRating > 0 ? userRating : null,
        personalNotes: personalNotes || null,
      });

      onClose();
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to create application');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-card" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2 className="modal-title">{isEdit ? 'Edit Application' : 'Add Application'}</h2>
          <button className="modal-close-btn" onClick={onClose} aria-label="Close modal">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="modal-body">
            {error && (
              <div style={{
                background: 'rgba(239, 68, 68, 0.1)',
                border: '1px solid var(--color-status-rejected)',
                color: 'var(--color-status-rejected)',
                padding: '8px 12px',
                borderRadius: 'var(--radius-md)',
                fontSize: 'var(--font-size-xs)'
              }}>
                {error}
              </div>
            )}

            {!isEdit && (
              <div className="modal-mode-tabs">
                <button
                  type="button"
                  className={`modal-mode-tab ${mode === 'custom' ? 'active' : ''}`}
                  onClick={() => setMode('custom')}
                >
                  + New Custom Opportunity
                </button>
                <button
                  type="button"
                  className={`modal-mode-tab ${mode === 'existing' ? 'active' : ''}`}
                  onClick={() => setMode('existing')}
                >
                  Select Existing Opportunity ({existingOpps.length})
                </button>
              </div>
            )}

            {!isEdit && mode === 'existing' ? (
              <div className="input-group">
                <label className="input-label">Select Opportunity *</label>
                <select
                  className="filter-select"
                  style={{ width: '100%' }}
                  value={selectedOppId}
                  onChange={(e) => setSelectedOppId(e.target.value)}
                  required
                >
                  <option value="">-- Choose Opportunity --</option>
                  {existingOpps.map((opp) => (
                    <option key={opp.id} value={opp.id}>
                      {opp.companyName} — {opp.title} ({opp.locationType})
                    </option>
                  ))}
                </select>
              </div>
            ) : !isEdit ? (
              <>
                <div className="form-grid-2">
                  <Input
                    label="Company Name *"
                    placeholder="e.g. Stripe, Vercel, Datadog"
                    value={companyName}
                    onChange={(e) => setCompanyName(e.target.value)}
                    required
                  />
                  <Input
                    label="Role Title *"
                    placeholder="e.g. Software Engineering Intern"
                    value={roleTitle}
                    onChange={(e) => setRoleTitle(e.target.value)}
                    required
                  />
                </div>

                <div className="form-grid-2">
                  <div className="input-group">
                    <label className="input-label">Work Mode</label>
                    <select
                      className="filter-select"
                      style={{ width: '100%' }}
                      value={locationType}
                      onChange={(e) => setLocationType(e.target.value)}
                    >
                      <option value="REMOTE">Remote</option>
                      <option value="HYBRID">Hybrid</option>
                      <option value="ONSITE">Onsite</option>
                    </select>
                  </div>

                  <Input
                    label="City / Location"
                    placeholder="e.g. San Francisco, CA"
                    value={locationCity}
                    onChange={(e) => setLocationCity(e.target.value)}
                  />
                </div>

                <Input
                  label="Hourly Stipend / Salary ($)"
                  type="number"
                  placeholder="e.g. 50"
                  value={stipendAmount}
                  onChange={(e) => setStipendAmount(e.target.value)}
                />
              </>
            ) : (
              <div className="form-grid-2">
                <div>
                  <label className="input-label">Company</label>
                  <div style={{ padding: '8px 0', fontWeight: 600 }}>{companyName}</div>
                </div>
                <div>
                  <label className="input-label">Role</label>
                  <div style={{ padding: '8px 0', fontWeight: 600 }}>{roleTitle}</div>
                </div>
              </div>
            )}

            <div className="form-grid-2">
              <div className="input-group">
                <label className="input-label">Application Stage</label>
                <select
                  className="filter-select"
                  style={{ width: '100%' }}
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                >
                  {STAGES.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
              </div>

              <div className="input-group">
                <label className="input-label">Priority / Rating</label>
                <div className="star-rating-selector">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <button
                      key={star}
                      type="button"
                      className={`star-select-btn ${star <= userRating ? 'active' : ''}`}
                      onClick={() => setUserRating(star === userRating ? 0 : star)}
                      title={`${star} Stars`}
                    >
                      ★
                    </button>
                  ))}
                </div>
              </div>
            </div>

            <div className="input-group">
              <label className="input-label">Personal Notes & Context</label>
              <textarea
                className="input"
                style={{ height: '80px', resize: 'vertical' }}
                placeholder="Add referral details, recruiter names, or interview thoughts..."
                value={personalNotes}
                onChange={(e) => setPersonalNotes(e.target.value)}
              />
            </div>
          </div>

          <div className="modal-footer">
            <Button variant="secondary" size="md" type="button" onClick={onClose} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button variant="primary" size="md" type="submit" loading={isSubmitting}>
              {isEdit ? 'Save Changes' : 'Create Application'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};