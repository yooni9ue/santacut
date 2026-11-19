package com.santacut.application.dossier.command

import java.util.UUID

/** 로그인한 참가자의 조서 조회. */
data class MyDossierCommand(val participantId: UUID)
