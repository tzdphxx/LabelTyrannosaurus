package com.labelhub.modules.storage;

import com.labelhub.modules.storage.dto.FileUploadResponse;
import com.labelhub.modules.storage.dto.SignedUrlResponse;
import com.labelhub.modules.storage.controller.FileController;
import com.labelhub.modules.storage.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileControllerTest {

    private final FileService fileService = mock(FileService.class);
    private final FileController fileController = new FileController(fileService);

    @Test
    void uploadDelegatesMultipartFileAndBusinessType() {
        MockMultipartFile file = new MockMultipartFile("file", "dataset.jsonl",
                "application/x-ndjson", "{\"id\":1}\n".getBytes());
        when(fileService.upload(file, "dataset")).thenReturn(new FileUploadResponse(
                99L,
                "dataset.jsonl",
                "application/x-ndjson",
                file.getSize(),
                "uploads/dataset/file.jsonl",
                "https://cos.example.com/download"
        ));

        var response = fileController.upload(file, "dataset");

        verify(fileService).upload(file, "dataset");
        assertThat(response.data().fileId()).isEqualTo(99L);
        assertThat(response.data().downloadUrl()).isEqualTo("https://cos.example.com/download");
    }

    @Test
    void signedUrlDelegatesFileId() {
        when(fileService.generateSignedUrl(99L)).thenReturn(new SignedUrlResponse(99L, "https://cos.example.com/signed"));

        var response = fileController.signedUrl(99L);

        verify(fileService).generateSignedUrl(99L);
        assertThat(response.data().fileId()).isEqualTo(99L);
        assertThat(response.data().downloadUrl()).isEqualTo("https://cos.example.com/signed");
    }
}
