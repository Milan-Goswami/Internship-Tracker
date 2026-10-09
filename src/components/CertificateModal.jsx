import { useEffect, useRef } from 'react';
import { X, ExternalLink, ShieldCheck, Award } from 'lucide-react';
import './CertificateModal.css';

export default function CertificateModal({ certificate, isOpen, onClose }) {
  const modalRef = useRef(null);
  const closeBtnRef = useRef(null);
  const lastActiveElementRef = useRef(null);

  useEffect(() => {
    if (isOpen) {
      lastActiveElementRef.current = document.activeElement;
      document.body.style.overflow = 'hidden';
      setTimeout(() => {
        closeBtnRef.current?.focus();
      }, 50);
    } else {
      document.body.style.overflow = '';
      if (lastActiveElementRef.current && typeof lastActiveElementRef.current.focus === 'function') {
        lastActiveElementRef.current.focus();
      }
    }
  }, [isOpen]);

  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
        return;
      }

      // Focus trap
      if (e.key === 'Tab' && modalRef.current) {
        const focusables = modalRef.current.querySelectorAll(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (focusables.length === 0) return;

        const first = focusables[0];
        const last = focusables[focusables.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === first) {
            e.preventDefault();
            last.focus();
          }
        } else {
          if (document.activeElement === last) {
            e.preventDefault();
            first.focus();
          }
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !certificate) return null;

  return (
    <div 
      className="cert-modal-overlay" 
      onClick={onClose} 
      role="dialog" 
      aria-modal="true" 
      aria-label={`Verified Credential: ${certificate.name}`}
      ref={modalRef}
    >
      <div 
        className="cert-modal-viewport" 
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top Header Bar */}
        <div className="cert-control-bar">
          <div className="cert-identity-group">
            <span className="cert-verified-pill">
              <ShieldCheck size={13} className="text-emerald" aria-hidden="true" />
              <span>VERIFIED CREDENTIAL</span>
            </span>
            <div className="cert-title-col">
              <strong className="cert-title-text">{certificate.name}</strong>
              <span className="cert-issuer-text">{certificate.issuer}</span>
            </div>
          </div>

          <div className="cert-actions-group">
            <a 
              href={certificate.asset} 
              target="_blank" 
              rel="noopener noreferrer"
              className="cert-btn-secondary" 
              title="Open full resolution asset in new tab"
              aria-label="Open full size asset in new tab"
            >
              <ExternalLink size={14} aria-hidden="true" />
              <span>Open Full Size</span>
            </a>

            <button 
              ref={closeBtnRef}
              type="button" 
              onClick={onClose} 
              className="cert-btn-close" 
              aria-label="Close certificate preview"
              title="Close (Escape)"
            >
              <X size={16} />
            </button>
          </div>
        </div>

        {/* Certificate Display Stage */}
        <div className="cert-display-stage">
          <div className="cert-image-chassis">
            <img 
              src={certificate.asset} 
              alt={`${certificate.name} certificate issued by ${certificate.issuer}`} 
              className="cert-original-image" 
              loading="eager"
            />
          </div>
        </div>

        {/* Bottom Details Footer */}
        <div className="cert-modal-footer">
          <div className="cert-meta-tag-cluster">
            <span className="cert-meta-tag tag-type">
              <Award size={12} className="meta-icon" aria-hidden="true" />
              <span>{certificate.type}</span>
            </span>
            {certificate.credentialId && (
              <span className="cert-meta-tag tag-id">
                <span>CREDENTIAL ID:</span>
                <code>{certificate.credentialId}</code>
              </span>
            )}
            {certificate.recipient && (
              <span className="cert-meta-tag tag-recipient">
                <span>RECIPIENT:</span>
                <strong>{certificate.recipient}</strong>
              </span>
            )}
          </div>
        </div>

      </div>
    </div>
  );
}
