package org.example.zentrio.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DriveFileDTO {

        private String id;
        private String name;
        private String mimeType;
        private String webViewLink;
        private boolean is_public;
}
