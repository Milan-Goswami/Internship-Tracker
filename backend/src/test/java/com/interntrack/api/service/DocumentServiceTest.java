package com.interntrack.api.service;

import com.interntrack.api.entity.Document;
import com.interntrack.api.entity.User;
import com.interntrack.api.entity.enums.DocumentType;
import com.interntrack.api.entity.enums.UserRole;
import com.interntrack.api.exception.ResourceNotFoundException;
import com.interntrack.api.repository.DocumentRepository;
import com.interntrack.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentService documentService;

    private User sampleUser;
    private Document sampleDocument;

    @BeforeEach
    void setUp() {
        sampleUser = new User("applicant@example.com", "hash123", "Applicant", UserRole.ROLE_USER);
        sampleDocument = new Document(
                sampleUser,
                "Backend Resume",
                DocumentType.RESUME,
                "https://drive.google.com/file/d/12345/view",
                "v1.0"
        );
    }

    @Test
    @DisplayName("findById returns document when found")
    void findById_whenFound_returnsDocument() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));

        Optional<Document> result = documentService.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Backend Resume");
        verify(documentRepository).findById(1L);
    }

    @Test
    @DisplayName("findById returns empty when document not found or null ID")
    void findById_whenMissing_returnsEmpty() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(documentService.findById(99L)).isEmpty();
        assertThat(documentService.findById(null)).isEmpty();
    }

    @Test
    @DisplayName("findAccessibleById returns document when requested by owner")
    void findAccessibleById_whenOwner_returnsDocument() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleDocument.setUser(user);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));

        Document result = documentService.findAccessibleById(1L, 10L);

        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Backend Resume");
    }

    @Test
    @DisplayName("findAccessibleById throws AccessDeniedException when requested by non-owner")
    void findAccessibleById_whenNonOwner_throwsAccessDeniedException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleDocument.setUser(owner);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));

        assertThatThrownBy(() -> documentService.findAccessibleById(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    @DisplayName("findAccessibleById throws ResourceNotFoundException when document missing")
    void findAccessibleById_whenMissing_throwsResourceNotFoundException() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.findAccessibleById(99L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Document not found with id: 99");
    }

    @Test
    @DisplayName("findAccessibleById throws IllegalArgumentException on null arguments")
    void findAccessibleById_whenNullArgs_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> documentService.findAccessibleById(null, 10L))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> documentService.findAccessibleById(1L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("findByUserId returns list of user documents")
    void findByUserId_returnsDocuments() {
        when(documentRepository.findByUserId(10L)).thenReturn(List.of(sampleDocument));

        List<Document> result = documentService.findByUserId(10L);

        assertThat(result).hasSize(1);
        verify(documentRepository).findByUserId(10L);
    }

    @Test
    @DisplayName("findByUserIdAndDocumentType returns documents filtered by type")
    void findByUserIdAndDocumentType_returnsFilteredDocuments() {
        when(documentRepository.findByUserIdAndDocumentType(10L, DocumentType.RESUME))
                .thenReturn(List.of(sampleDocument));

        List<Document> result = documentService.findByUserIdAndDocumentType(10L, DocumentType.RESUME);

        assertThat(result).hasSize(1);
        verify(documentRepository).findByUserIdAndDocumentType(10L, DocumentType.RESUME);
    }

    @Test
    @DisplayName("createDocument successfully creates and saves valid document metadata")
    void createDocument_whenValid_createsAndSaves() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Document created = documentService.createDocument(
                1L,
                "  Software Resume 2026  ",
                DocumentType.RESUME,
                "https://dropbox.com/s/abcdef/resume.pdf",
                "  2026-v2  "
        );

        assertThat(created).isNotNull();
        assertThat(created.getUser()).isEqualTo(sampleUser);
        assertThat(created.getTitle()).isEqualTo("Software Resume 2026");
        assertThat(created.getDocumentType()).isEqualTo(DocumentType.RESUME);
        assertThat(created.getStoragePath()).isEqualTo("https://dropbox.com/s/abcdef/resume.pdf");
        assertThat(created.getVersionTag()).isEqualTo("2026-v2");

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("Software Resume 2026");
    }

    @Test
    @DisplayName("createDocument throws ResourceNotFoundException when user missing")
    void createDocument_whenUserMissing_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.createDocument(
                99L, "Title", DocumentType.RESUME, "https://example.com/doc.pdf", null
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDocument throws IllegalArgumentException on blank title")
    void createDocument_whenBlankTitle_throwsException() {
        assertThatThrownBy(() -> documentService.createDocument(
                1L, "   ", DocumentType.RESUME, "https://example.com/doc.pdf", null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createDocument throws IllegalArgumentException on missing storagePath")
    void createDocument_whenMissingStoragePath_throwsException() {
        assertThatThrownBy(() -> documentService.createDocument(
                1L, "Title", DocumentType.RESUME, null, null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> documentService.createDocument(
                1L, "Title", DocumentType.RESUME, "   ", null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createDocument throws IllegalArgumentException on invalid storagePath string")
    void createDocument_whenInvalidUrlString_throwsException() {
        assertThatThrownBy(() -> documentService.createDocument(
                1L, "Title", DocumentType.RESUME, "not-a-valid-url", null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> documentService.createDocument(
                1L, "Title", DocumentType.RESUME, "/local/server/path/resume.pdf", null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createDocument throws IllegalArgumentException on unsupported URL scheme")
    void createDocument_whenUnsupportedScheme_throwsException() {
        assertThatThrownBy(() -> documentService.createDocument(
                1L, "Title", DocumentType.RESUME, "file:///C:/Users/test/resume.pdf", null
        )).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> documentService.createDocument(
                1L, "Title", DocumentType.RESUME, "ftp://example.com/files/resume.pdf", null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateDocument updates fields when requested by owner")
    void updateDocument_whenOwner_updatesFields() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleDocument.setUser(user);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Document updated = documentService.updateDocument(
                1L,
                10L,
                "Updated Resume Title",
                DocumentType.PORTFOLIO,
                "https://myportfolio.dev/resume.pdf",
                "final-v3"
        );

        assertThat(updated.getTitle()).isEqualTo("Updated Resume Title");
        assertThat(updated.getDocumentType()).isEqualTo(DocumentType.PORTFOLIO);
        assertThat(updated.getStoragePath()).isEqualTo("https://myportfolio.dev/resume.pdf");
        assertThat(updated.getVersionTag()).isEqualTo("final-v3");
    }

    @Test
    @DisplayName("updateDocument throws AccessDeniedException when non-owner requests update")
    void updateDocument_whenNonOwner_throwsException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleDocument.setUser(owner);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));

        // User 99 tries to update User 10's document
        assertThatThrownBy(() -> documentService.updateDocument(
                1L, 99L, "Hacked Title", null, null, null
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized");

        verify(documentRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteDocument deletes document when requested by owner")
    void deleteDocument_whenOwner_deletesDocument() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        sampleDocument.setUser(user);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));

        documentService.deleteDocument(1L, 10L);

        verify(documentRepository).delete(sampleDocument);
    }

    @Test
    @DisplayName("deleteDocument throws AccessDeniedException when non-owner requests delete")
    void deleteDocument_whenNonOwner_throwsException() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(10L);
        sampleDocument.setUser(owner);

        when(documentRepository.findById(1L)).thenReturn(Optional.of(sampleDocument));

        assertThatThrownBy(() -> documentService.deleteDocument(1L, 99L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Unauthorized");

        verify(documentRepository, never()).delete(any());
    }
}