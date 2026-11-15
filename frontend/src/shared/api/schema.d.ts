export interface paths {
    "/v1/shares": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Create a share link for my latest result
         * @description Creates or reuses a revocable public link containing a snapshot of the current result. The JSON response includes the HTML page URL and its 1200×630 PNG image URL. Fetch a CSRF token first.
         */
        post: operations["createShare"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/participants": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Register a participant
         * @description Creates a participant and session. Nicknames are normalized with NFKC and compared case-insensitively. Fetch a CSRF token first. The session credential is returned only as an HttpOnly cookie, never in the JSON body.
         */
        post: operations["registerParticipant"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/analyses": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Submit an analysis
         * @description Queues one analysis and returns immediately. Provide 20–2,000 normalized events; each text is 1–2,000 characters and total text is at most 200,000 characters. Event timestamps must be within the last 90 days and no more than 5 minutes in the future. A participant may submit at most 3 jobs per rolling 24 hours. Raw platform export files are not accepted. Reusing an idempotency key with the same body returns the original job; a different body conflicts. The AI evaluates care/empathy (30%), positivity/support (25%), respectful communication (25%), and cooperation/sharing (20%); the server validates evidence references and computes the weighted total.
         */
        post: operations["submitAnalysis"];
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/regions": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List supported regions
         * @description Returns the Seoul districts currently supported for participant registration.
         */
        get: operations["listRegions"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/rankings": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get the ranking page
         * @description Returns at most 50 participants ordered by delivery sequence. offset is zero-based and limited to 0–10,000. score_hundredths is an integer number of hundredths (for example, 8250 means 82.50 points). eta and computed_at are ISO-8601 UTC timestamps; render ETA in Asia/Seoul. Delivery simulation starts at 00:00 Asia/Seoul on December 25, visits each registered district's representative coordinate in delivery order, assumes 100 km/h travel, 3 minutes per stop, and 6 active hours per day. This is a game simulation, not an actual delivery promise.
         */
        get: operations["getRankings"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/me": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get the signed-in participant
         * @description Returns the participant associated with the HttpOnly session cookie. Use this on page load to distinguish an active session from an anonymous browser.
         */
        get: operations["getCurrentParticipant"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/me/result": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get my latest result
         * @description Returns the latest persisted assessment and the latest published ranking snapshot for the authenticated participant. score_hundredths is an integer divided by 100 for display; eta and computed_at are ISO-8601 UTC timestamps.
         */
        get: operations["getMyResult"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/csrf": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get a CSRF token
         * @description Call before any POST or DELETE request. The response also sets the XSRF-TOKEN cookie; echo token in the header named by headerName.
         */
        get: operations["getCsrfToken"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/analyses/{id}/events": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Stream analysis progress and result
         * @description Server-Sent Events stream, polled every second for up to 150 seconds. Event names: analysis (job status), result (final score/rank/ETA), error (terminal failure). The client should reconnect only while the job is non-terminal. Only the job owner can subscribe.
         */
        get: operations["streamAnalysisEvents"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/s/{token}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * View a public share page
         * @description Returns a server-rendered HTML page with Open Graph metadata for social previews. Contains the immutable result snapshot captured when the share was created.
         */
        get: operations["getPublicSharePage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/s/{token}/image.png": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get the public share image
         * @description Returns a generated 1200×630 PNG card for social sharing. The image contains the saved share snapshot, not a live ranking query.
         */
        get: operations["getPublicShareImage"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/v1/shares/{token}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        post?: never;
        /**
         * Revoke my share link
         * @description Deletes the share token. Its public HTML and PNG URLs then return 404. Fetch a CSRF token first.
         */
        delete: operations["revokeShare"];
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        CreateShareResponse: {
            /** @description 43-character base64url bearer token. Keep private; anyone with it can view the result. */
            token: string;
            /**
             * Format: uri
             * @description Public HTML result page URL
             */
            url: string;
            /**
             * Format: uri
             * @description Public 1200×630 PNG card URL
             */
            imageUrl: string;
        };
        ApiErrorResponse: {
            /**
             * Format: date-time
             * @description Error time in UTC
             * @example 2026-11-15T12:00:00Z
             */
            timestamp: string;
            /**
             * Format: int32
             * @description HTTP status code
             * @example 400
             */
            status: number;
            /**
             * @description HTTP reason phrase
             * @example Bad Request
             */
            error: string;
            /**
             * @description Request path
             * @example /v1/analyses
             */
            path: string;
        };
        ParticipantRequest: {
            /**
             * @description Unique nickname. NFKC-normalized; 2–16 Korean/Latin letters, digits or underscores.
             * @example 산타도와줘
             */
            nickname: string;
            /**
             * @description One of the region codes returned by GET /v1/regions
             * @example KR-11-650
             */
            regionCode: string;
        };
        ParticipantResponse: {
            /**
             * Format: uuid
             * @description New participant UUID
             * @example 7af6f50a-4a88-4c32-9be5-a210a8ce1234
             */
            id: string;
            /**
             * @description Canonical nickname
             * @example 산타도와줘
             */
            nickname: string;
            /**
             * @description Registered region code
             * @example KR-11-650
             */
            regionCode: string;
        };
        AnalysisInput: {
            /**
             * @description Export platform
             * @example KAKAO
             * @enum {string}
             */
            source: "KAKAO" | "INSTAGRAM";
            /** @description 20–2,000 normalized events, ordered by occurredAt when possible */
            events: components["schemas"]["EventInput"][];
        };
        EventInput: {
            /**
             * Format: date-time
             * @description Event time as an ISO-8601 UTC timestamp
             * @example 2026-12-01T08:30:00Z
             */
            occurredAt: string;
            /**
             * @description Message text or normalized activity description; 1–2,000 characters
             * @example 친구가 힘들어해서 먼저 연락하고 이야기를 들어줬어.
             */
            text: string;
        };
        AnalysisSubmissionResponse: {
            /**
             * Format: uuid
             * @description Analysis job UUID
             * @example 9ef3fca1-ec60-4c43-a231-d378d246ee34
             */
            id: string;
            /**
             * @description Initial state, or existing state for an idempotent replay
             * @example QUEUED
             * @enum {string}
             */
            status: "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "INSUFFICIENT_DATA";
        };
        RegionResponse: {
            /**
             * @description Stable region code used for registration
             * @example KR-11-650
             */
            code: string;
            /**
             * @description Korean display name
             * @example 영등포구
             */
            name: string;
        };
        RankingResponse: {
            /** @description Participant nickname */
            nickname: string;
            /**
             * Format: int32
             * @description Competition rank; equal scores share a rank
             */
            rank: number;
            /**
             * Format: date-time
             * @description Simulated delivery ETA in UTC
             */
            eta: string;
            /**
             * Format: int32
             * @description Weighted score in hundredths; divide by 100 for points
             */
            score_hundredths: number;
            /**
             * Format: date-time
             * @description Time the ranking snapshot was calculated in UTC
             */
            computed_at: string;
        };
        CurrentParticipantResponse: {
            /**
             * Format: uuid
             * @description Authenticated participant UUID
             */
            id: string;
            /** @description Registered nickname */
            nickname: string;
            /**
             * @description Registered region code
             * @example KR-11-650
             */
            regionCode: string;
        };
        MyResultResponse: {
            /**
             * Format: int32
             * @description Competition rank; equal scores share a rank
             */
            rank: number;
            /**
             * Format: date-time
             * @description Simulated delivery ETA in UTC
             */
            eta: string;
            /** @description Playful score verdict */
            verdict: string;
            /** @description Playful explanation */
            comment: string;
            /**
             * Format: int32
             * @description Delivery order among participants
             */
            delivery_sequence: number;
            /**
             * Format: date-time
             * @description Time the ranking snapshot was calculated in UTC
             */
            computed_at: string;
            /**
             * Format: int32
             * @description Weighted score in hundredths; divide by 100 for points
             * @example 8245
             */
            score_hundredths: number;
        };
        CsrfResponse: {
            /**
             * @description Request header name to send, usually X-XSRF-TOKEN
             * @example X-XSRF-TOKEN
             */
            headerName: string;
            /**
             * @description CSRF token value; echo in headerName and retain the XSRF-TOKEN cookie
             * @example e9d3...f61a
             */
            token: string;
        };
        AnalysisStatusResponse: {
            /**
             * Format: uuid
             * @description Analysis job UUID
             */
            id: string;
            /**
             * @description Current job state
             * @enum {string}
             */
            status: "QUEUED" | "RUNNING" | "SUCCEEDED" | "FAILED" | "INSUFFICIENT_DATA";
            /** @description Stable machine-readable error code; null until failed */
            error_code: string;
            /** @description Assessment UUID after successful analysis */
            assessment_id: string;
            /**
             * Format: date-time
             * @description Job creation time in UTC
             */
            created_at: string;
            /**
             * Format: date-time
             * @description Terminal transition time in UTC; null while queued or running
             */
            finished_at: string;
        };
        AnalysisResultResponse: {
            /** @description Playful score verdict */
            verdict: string;
            /** @description Playful explanation */
            comment: string;
            /**
             * Format: int32
             * @description Competition rank; equal scores share a rank
             */
            rank: number;
            /**
             * Format: date-time
             * @description Simulated delivery ETA in UTC
             * @example 2026-12-25T03:15:00.000Z
             */
            eta: string;
            /**
             * Format: int32
             * @description Weighted score in hundredths; divide by 100 for points
             * @example 8245
             */
            score_hundredths: number;
            /**
             * Format: int32
             * @description Delivery order among participants
             */
            delivery_sequence: number;
        };
    };
    responses: never;
    parameters: never;
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    createShare: {
        parameters: {
            query?: never;
            header: {
                /** @description Token returned by GET /v1/csrf. */
                "X-XSRF-TOKEN": string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Share link created or reused */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CreateShareResponse"];
                };
            };
            /** @description Session missing, invalid, or expired */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description CSRF token missing or invalid */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description No ranked result is available to share */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    registerParticipant: {
        parameters: {
            query?: never;
            header: {
                /** @description Token returned by GET /v1/csrf. Keep the XSRF-TOKEN cookie from that response. */
                "X-XSRF-TOKEN": string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["ParticipantRequest"];
            };
        };
        responses: {
            /** @description Participant created; session credential is set as an HttpOnly cookie */
            201: {
                headers: {
                    /** @description Session cookie; sc_session locally or __Host-sc_session in production */
                    "Set-Cookie"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ParticipantResponse"];
                };
            };
            /** @description Invalid nickname, malformed request, or unsupported region */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description CSRF token missing or invalid */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Nickname is already in use */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    submitAnalysis: {
        parameters: {
            query?: never;
            header: {
                /** @description 16–100 ASCII letters, digits, underscore or hyphen; reuse only for the same request body. */
                "Idempotency-Key": string;
                /** @description Token returned by GET /v1/csrf. */
                "X-XSRF-TOKEN": string;
            };
            path?: never;
            cookie?: never;
        };
        requestBody: {
            content: {
                "application/json": components["schemas"]["AnalysisInput"];
            };
        };
        responses: {
            /** @description Job queued */
            202: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["AnalysisSubmissionResponse"];
                };
            };
            /** @description Malformed request, validation failure, invalid idempotency key, or out-of-range event timestamp */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Session missing, invalid, or expired */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description CSRF token missing or invalid */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Idempotency key reused with a different body, or season is frozen */
            409: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Participant daily submission limit reached */
            429: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description AI model is not configured */
            503: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    listRegions: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Supported region codes and display names */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RegionResponse"][];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    getRankings: {
        parameters: {
            query?: {
                /** @description Zero-based row offset (default 0; range 0–10,000). */
                offset?: number;
            };
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Ranking rows for the requested page */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["RankingResponse"][];
                };
            };
            /** @description Offset is outside 0–10,000 */
            400: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Session missing, invalid, or expired */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    getCurrentParticipant: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Current participant profile */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CurrentParticipantResponse"];
                };
            };
            /** @description Session missing, invalid, or expired */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Session participant no longer exists */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    getMyResult: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Latest score, ranking, and simulated delivery time */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MyResultResponse"];
                };
            };
            /** @description Session missing, invalid, or expired */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description No ranked result is available yet */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    getCsrfToken: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description CSRF header name and token */
            200: {
                headers: {
                    /** @description XSRF-TOKEN cookie required for subsequent write requests */
                    "Set-Cookie"?: string;
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["CsrfResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    streamAnalysisEvents: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description UUID returned by POST /v1/analyses */
                id: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description text/event-stream. Event names: analysis and error use AnalysisStatusResponse; result uses AnalysisResultResponse. */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "text/event-stream": components["schemas"]["AnalysisStatusResponse"] | components["schemas"]["AnalysisResultResponse"];
                };
            };
            /** @description Session missing, invalid, or expired */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Job does not exist or belongs to another participant */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    getPublicSharePage: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 43-character base64url share token. */
                token: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description HTML page with Open Graph metadata */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "text/html": string;
                };
            };
            /** @description Share token is invalid, revoked, or unknown */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    getPublicShareImage: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description 43-character base64url share token. */
                token: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description PNG result card */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "image/png": string;
                };
            };
            /** @description Share token is invalid, revoked, or unknown */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
    revokeShare: {
        parameters: {
            query?: never;
            header: {
                /** @description Token returned by GET /v1/csrf. */
                "X-XSRF-TOKEN": string;
            };
            path: {
                /** @description 43-character base64url share token. */
                token: string;
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Share link revoked */
            204: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
            /** @description Session missing, invalid, or expired */
            401: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description CSRF token missing or invalid */
            403: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Share token does not exist or belongs to another participant */
            404: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
            /** @description Unexpected server error */
            500: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["ApiErrorResponse"];
                };
            };
        };
    };
}
