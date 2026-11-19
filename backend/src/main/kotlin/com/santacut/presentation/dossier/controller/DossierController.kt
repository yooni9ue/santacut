package com.santacut.presentation.dossier.controller

import com.santacut.application.dossier.command.MyDossierCommand
import com.santacut.application.dossier.facade.DossierFacade
import com.santacut.presentation.dossier.docs.GetMyDossierApi
import com.santacut.presentation.dossier.response.DossierResponse
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class DossierController(
    private val dossierFacade: DossierFacade,
) {
    @GetMapping("/v1/me/dossier")
    @GetMyDossierApi
    fun myDossier(@AuthenticationPrincipal @Parameter(hidden = true) participantId: UUID): DossierResponse =
        DossierResponse.from(dossierFacade.getMyDossier(MyDossierCommand(participantId)))
}
