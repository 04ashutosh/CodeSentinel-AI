package com.codesentinel.ingestion.event;

import com.codesentinel.common.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class IngestionCompletedEvent extends BaseEvent {

    private Long projectId;
    private String projectName;
    private String storagePath;
    private String userEmail;
}