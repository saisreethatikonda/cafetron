package com.cafetron.admin.service;

import com.cafetron.admin.dto.OpsStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationsService {

    private final WindowService windowService;

    public OpsStatusDTO getStatus() {
        return windowService.getStatus();
    }

    public OpsStatusDTO toggleWindow() {
        return windowService.toggleWindow();
    }

    public OpsStatusDTO updateCutoff(String timeStr) {
        return windowService.updateCutoff(timeStr);
    }
}