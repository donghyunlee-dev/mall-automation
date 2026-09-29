package com.sfood.automation.common_code.presentation;

import com.sfood.automation.common_code.application.dto.CommonCodeListResponse;
import com.sfood.automation.common_code.application.dto.CommonCodeRequest;
import com.sfood.automation.common_code.application.usecase.CommonCodeUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/common-codes")
public class CommonCodeController {

    private final CommonCodeUseCase commonCodeUseCase;

    public CommonCodeController(CommonCodeUseCase commonCodeUseCase) {
        this.commonCodeUseCase = commonCodeUseCase;
    }

    @GetMapping
    public ResponseEntity<CommonCodeListResponse> findAll(
            @RequestParam(required = false) String groupCode,
            @RequestParam(required = false) String useYn) {
        return ResponseEntity.ok(commonCodeUseCase.findAll(groupCode, useYn));
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody CommonCodeRequest request) {
        commonCodeUseCase.create(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Integer id,
                                        @RequestBody CommonCodeRequest request) {
        commonCodeUseCase.update(id, request);
        return ResponseEntity.ok().build();
    }
}
