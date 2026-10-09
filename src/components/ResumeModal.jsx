import { useEffect, useRef } from 'react';
import { X, ExternalLink, Download, FileText } from 'lucide-react';
import './ResumeModal.css';

export default function ResumeModal({ isOpen, onClose }) {
  const modalRef = useRef(null);
  const closeBtnRef = useRef(null);
  const lastActiveElementRef = useRef(null);

  // Store trigger element when opened to restore focus on close
  useEffect(() => {
    if (isOpen) {
      lastActiveElementRef.current = document.activeElement;
      document.body.style.overflow = 'hidden';
      
      // Focus the close button after modal renders
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

  // Handle keyboard events (ESC and Focus Trap)
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose();
        return;
      }

      // Simple focus trap
      if (e.key === 'Tab' && modalRef.current) {
        const focusableElements = modalRef.current.querySelectorAll(
          'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
        );
        if (focusableElements.length === 0) return;

        const firstElement = focusableElements[0];
        const lastElement = focusableElements[focusableElements.length - 1];

        if (e.shiftKey) {
          if (document.activeElement === firstElement) {
            e.preventDefault();
            lastElement.focus();
          }
        } else {
          if (document.activeElement === lastElement) {
            e.preventDefault();
            firstElement.focus();
          }
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  return (
    <div 
      className="resume-modal-overlay" 
      onClick={onClose} 
      role="dialog" 
      aria-modal="true" 
      aria-label="Milan Goswami Professional Resume PDF Viewer"
      ref={modalRef}
    >
      <div 
        className="resume-modal-viewport" 
        onClick={(e) => e.stopPropagation()}
      >
        {/* Top Control Bar */}
        <div className="resume-control-bar">
          <div className="resume-doc-tag">
            <span className="doc-live-indicator"></span>
            <FileText size={14} className="doc-icon-emerald" aria-hidden="true" />
            <span className="doc-tag-label">MILAN_GOSWAMI_RESUME.PDF</span>
          </div>

          <div className="resume-actions-group">
            <a 
              href="/milan-goswami-resume.pdf" 
              target="_blank" 
              rel="noopener noreferrer"
              className="resume-btn-secondary" 
              title="Open real PDF in new browser tab"
              aria-label="Open PDF in new tab"
            >
              <ExternalLink size={14} aria-hidden="true" />
              <span>Open in New Tab</span>
            </a>

            <a 
              href="/milan-goswami-resume.pdf" 
              download="Milan_Goswami_Resume.pdf"
              className="resume-btn-secondary" 
              title="Download original PDF"
              aria-label="Download original PDF"
            >
              <Download size={14} aria-hidden="true" />
              <span>Download PDF</span>
            </a>

            <button 
              ref={closeBtnRef}
              type="button" 
              onClick={onClose} 
              className="resume-btn-close" 
              aria-label="Close resume viewer"
              title="Close (Escape)"
            >
              <X size={16} />
            </button>
          </div>
        </div>

        {/* Real PDF Document Stage */}
        <div className="resume-pdf-stage">
          <iframe 
            src="/milan-goswami-resume.pdf#toolbar=1&navpanes=0&view=FitH" 
            title="Milan Goswami Authentic Resume PDF"
            className="resume-pdf-frame"
          />

          {/* Mobile Fallback Card */}
          <div className="resume-mobile-pdf-notice">
            <p>Viewing on a mobile device without inline PDF support?</p>
            <a 
              href="/milan-goswami-resume.pdf" 
              target="_blank" 
              rel="noopener noreferrer"
              className="resume-mobile-open-btn"
            >
              <ExternalLink size={14} />
              <span>Open Full PDF Document</span>
            </a>
          </div>
        </div>

      </div>
    </div>
  );
}
