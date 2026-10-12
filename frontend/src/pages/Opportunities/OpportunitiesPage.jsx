import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Skeleton } from '../../components/common/Skeleton';
import { EmptyState } from '../../components/states/EmptyState';
import { ErrorState } from '../../components/states/ErrorState';
import { useToast } from '../../context/ToastContext';
import { opportunityService } from '../../services/opportunityService';
import { applicationService } from '../../services/applicationService';

import { OpportunityFilterBar } from './components/OpportunityFilterBar';
import { OpportunityCard } from './components/OpportunityCard';
import { SkillMatchDrawer } from './components/SkillMatchDrawer';
import { OpportunityModal } from './components/OpportunityModal';
import { DeleteOpportunityModal } from './components/DeleteOpportunityModal';

import './OpportunitiesPage.css';

export const OpportunitiesCatalog = () => {
  const [opportunities, setOpportunities] = useState([]);
  const [applications, setApplications] = useState([]);
  const [companies, setCompanies] = useState([]);
  const [skillMatches, setSkillMatches] = useState({});

  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filters & Search
  const [searchQuery, setSearchQuery] = useState('');
  const [locationFilter, setLocationFilter] = useState('ALL');
  const [sourceFilter, setSourceFilter] = useState('ALL');
  const [matchFilter, setMatchFilter] = useState('ALL');

  // Drawer
  const [drawerOpportunity, setDrawerOpportunity] = useState(null);

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalInitialData, setModalInitialData] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Application conversion in flight
  const [convertingId, setConvertingId] = useState(null);

  const { showToast } = useToast();

  const loadData = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [oppsData, appsData, compsData] = await Promise.all([
        opportunityService.getOpportunities(),
        applicationService.getApplications().catch(() => []),
        opportunityService.getCompanies().catch(() => []),
      ]);

      const oppsList = oppsData || [];
      setOpportunities(oppsList);
      setApplications(appsData || []);
      setCompanies(compsData || []);

      // Batch fetch skill matches for top initial opportunities (capped to avoid network flood)
      const initialBatch = oppsList.slice(0, 8);
      if (initialBatch.length > 0) {
        const matchResults = await Promise.allSettled(
          initialBatch.map((o) => opportunityService.getOpportunitySkillMatch(o.id))
        );
        const batchMatches = {};
        matchResults.forEach((res, idx) => {
          if (res.status === 'fulfilled' && res.value) {
            batchMatches[initialBatch[idx].id] = res.value;
          }
        });
        setSkillMatches((prev) => ({ ...prev, ...batchMatches }));
      }
    } catch (err) {
      setError(err.response?.data?.message || err.message || 'Failed to load opportunities');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Map opportunityId to existing pipeline application
  const applicationsByOppId = useMemo(() => {
    const map = {};
    applications.forEach((app) => {
      if (app.opportunityId) {
        map[app.opportunityId] = app;
      }
    });
    return map;
  }, [applications]);

  // Filtering
  const filteredOpportunities = useMemo(() => {
    return opportunities.filter((opp) => {
      // 1. Search Query
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase();
        const title = (opp.title || '').toLowerCase();
        const comp = (opp.companyName || '').toLowerCase();
        const city = (opp.locationCity || '').toLowerCase();
        const desc = (opp.description || '').toLowerCase();
        if (!title.includes(query) && !comp.includes(query) && !city.includes(query) && !desc.includes(query)) {
          return false;
        }
      }

      // 2. Location Mode Filter
      if (locationFilter !== 'ALL' && opp.locationType !== locationFilter) {
        return false;
      }

      // 3. Source Filter
      if (sourceFilter === 'GLOBAL' && opp.isCustom) {
        return false;
      }
      if (sourceFilter === 'CUSTOM' && !opp.isCustom) {
        return false;
      }

      // 4. Match Filter
      if (matchFilter !== 'ALL') {
        const match = skillMatches[opp.id];
        if (!match) return false;
        if (matchFilter === 'HIGH' && (match.zeroSkillsSpecified || match.matchPercentage == null || match.matchPercentage < 70)) {
          return false;
        }
        if (matchFilter === 'MATCHED' && (match.zeroSkillsSpecified || match.matchPercentage == null || match.matchPercentage <= 0)) {
          return false;
        }
      }

      return true;
    });
  }, [opportunities, searchQuery, locationFilter, sourceFilter, matchFilter, skillMatches]);

  // Clear filters
  const handleClearFilters = () => {
    setSearchQuery('');
    setLocationFilter('ALL');
    setSourceFilter('ALL');
    setMatchFilter('ALL');
  };

  // Convert opportunity to Application
  const handleConvert = async (opp) => {
    setConvertingId(opp.id);
    try {
      const createdApp = await applicationService.createApplication({
        opportunityId: opp.id,
        status: 'APPLIED',
      });
      setApplications((prev) => [...prev, createdApp]);
      showToast(`Added "${opp.title}" to your application pipeline!`, 'success');
    } catch (err) {
      if (err.response?.status === 409) {
        showToast('Application already exists in your pipeline', 'warning');
      } else {
        showToast(err.response?.data?.message || 'Failed to track application', 'error');
      }
    } finally {
      setConvertingId(null);
    }
  };

  // Create / Edit opportunity
  const handleOpenAddModal = () => {
    setModalInitialData(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (opp) => {
    setModalInitialData(opp);
    setIsModalOpen(true);
  };

  const handleSaveOpportunity = async (payload, isEdit, id) => {
    if (isEdit) {
      const updated = await opportunityService.updateOpportunity(id, payload);
      setOpportunities((prev) => prev.map((o) => (o.id === id ? { ...o, ...updated } : o)));
      if (drawerOpportunity?.id === id) {
        setDrawerOpportunity((prev) => ({ ...prev, ...updated }));
      }
      showToast('Opportunity updated successfully', 'success');
    } else {
      const created = await opportunityService.createOpportunity(payload);
      setOpportunities((prev) => [created, ...prev]);
      showToast('Custom opportunity created successfully', 'success');
      // Fetch skill match for newly created opportunity
      try {
        const match = await opportunityService.getOpportunitySkillMatch(created.id);
        setSkillMatches((prev) => ({ ...prev, [created.id]: match }));
      } catch {
        // Ignore
      }
    }
  };

  // Delete opportunity
  const handleDeleteOpportunity = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);
    try {
      await opportunityService.deleteOpportunity(deleteTarget.id);
      setOpportunities((prev) => prev.filter((o) => o.id !== deleteTarget.id));
      if (drawerOpportunity?.id === deleteTarget.id) {
        setDrawerOpportunity(null);
      }
      showToast('Opportunity deleted successfully', 'success');
      setDeleteTarget(null);
    } catch (err) {
      showToast(err.response?.data?.message || 'Failed to delete opportunity', 'error');
    } finally {
      setIsDeleting(false);
    }
  };

  // Open drawer and fetch skill match on demand if not already cached
  const handleOpenDrawer = async (opp) => {
    setDrawerOpportunity(opp);
    if (!skillMatches[opp.id]) {
      try {
        const match = await opportunityService.getOpportunitySkillMatch(opp.id);
        setSkillMatches((prev) => ({ ...prev, [opp.id]: match }));
      } catch {
        // Ignore
      }
    }
  };

  // Callback when skills are updated in drawer
  const handleSkillsUpdated = async () => {
    if (drawerOpportunity) {
      try {
        const match = await opportunityService.getOpportunitySkillMatch(drawerOpportunity.id);
        setSkillMatches((prev) => ({ ...prev, [drawerOpportunity.id]: match }));
      } catch {
        // Ignore
      }
    }
  };

  return (
    <div className="opportunities-page">
      <header className="opportunities-page-header">
        <h1 className="opportunities-page-title">Opportunities Catalog</h1>
        <p className="opportunities-page-subtitle">
          Browse verified job specifications, analyze deterministic skill gaps, and track opportunities directly into your pipeline.
        </p>
      </header>

      <OpportunityFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        locationFilter={locationFilter}
        onLocationFilterChange={setLocationFilter}
        sourceFilter={sourceFilter}
        onSourceFilterChange={setSourceFilter}
        matchFilter={matchFilter}
        onMatchFilterChange={setMatchFilter}
        onClearFilters={handleClearFilters}
        onAddClick={handleOpenAddModal}
        filteredCount={filteredOpportunities.length}
        totalCount={opportunities.length}
      />

      {isLoading ? (
        <div className="opportunities-loading-container">
          <div className="opportunities-loading-grid">
            <Skeleton width="100%" height="220px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="220px" style={{ borderRadius: '12px' }} />
            <Skeleton width="100%" height="220px" style={{ borderRadius: '12px' }} />
          </div>
        </div>
      ) : error ? (
        <ErrorState
          title="Failed to Load Opportunities"
          description={error}
          onRetry={loadData}
        />
      ) : opportunities.length === 0 ? (
        <EmptyState
          title="No Opportunities Cataloged Yet"
          description="Your catalog is currently empty. Add your first custom internship opportunity to start evaluating skill alignment and managing applications."
          actionLabel="+ Add Custom Opportunity"
          onAction={handleOpenAddModal}
        />
      ) : filteredOpportunities.length === 0 ? (
        <EmptyState
          title="No Matching Opportunities"
          description="No opportunities match your current filters and search terms. Try clearing or broadening your search."
          actionLabel="Reset Filters"
          onAction={handleClearFilters}
        />
      ) : (
        <div className="opportunities-grid">
          {filteredOpportunities.map((opp) => (
            <OpportunityCard
              key={opp.id}
              opportunity={opp}
              pipelineApplication={applicationsByOppId[opp.id]}
              skillMatch={skillMatches[opp.id]}
              onOpenDrawer={handleOpenDrawer}
              onConvert={handleConvert}
              onEdit={handleOpenEditModal}
              onDelete={(item) => setDeleteTarget(item)}
              isConverting={convertingId === opp.id}
            />
          ))}
        </div>
      )}

      {/* Slide-over Skill Match Detail Drawer */}
      <SkillMatchDrawer
        opportunity={drawerOpportunity}
        isOpen={Boolean(drawerOpportunity)}
        onClose={() => setDrawerOpportunity(null)}
        pipelineApplication={drawerOpportunity ? applicationsByOppId[drawerOpportunity.id] : null}
        onConvert={handleConvert}
        isConverting={drawerOpportunity ? convertingId === drawerOpportunity.id : false}
        onSkillsUpdated={handleSkillsUpdated}
      />

      {/* Add / Edit Custom Opportunity Modal */}
      <OpportunityModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSave={handleSaveOpportunity}
        initialData={modalInitialData}
        companies={companies}
        onCreateCompany={(data) => opportunityService.createCompany(data)}
      />

      {/* Delete Opportunity Confirmation Modal */}
      <DeleteOpportunityModal
        isOpen={Boolean(deleteTarget)}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteOpportunity}
        opportunity={deleteTarget}
        isDeleting={isDeleting}
      />
    </div>
  );
};

export const OpportunitiesPage = OpportunitiesCatalog;
export default OpportunitiesCatalog;
