-- 앱이 뜰 때마다 Hibernate가 테이블을 만든 다음(spring.jpa.defer-datasource-initialization) 맨 먼저 실행되는 시드다.
-- 모든 문장이 IF NOT EXISTS 또는 INSERT OR IGNORE라 몇 번을 다시 실행해도 결과가 같고, 기존 DB에는 없는 것만 더한다.

-- Hibernate의 SQLite 방언은 UNIQUE 제약(@UniqueConstraint)과 유일 인덱스를 만들지 않으므로 여기서 만든다.
-- 예전 schema.sql로 만든 DB에는 같은 열의 UNIQUE 제약이 이미 있어 이름 붙은 인덱스가 하나 더 생긴다. 동작은 같다.
CREATE UNIQUE INDEX IF NOT EXISTS analysis_jobs_user_idempotency_key ON analysis_jobs(user_id,idempotency_key);
CREATE UNIQUE INDEX IF NOT EXISTS prompts_name_version ON prompts(name,version);
-- 같은 순위 스냅숏의 공유 링크는 참가자마다 하나뿐이다. 공유 취소는 행을 지우므로 엔티티에 revoked_at이 없다.
-- 예전 DB에는 같은 이름의 인덱스가 `WHERE revoked_at IS NULL`(늘 NULL인 컬럼)로 이미 있어 이 문장은 아무것도 하지 않는다.
CREATE UNIQUE INDEX IF NOT EXISTS shares_active_snapshot_unique ON shares(user_id,snapshot_id);

-- 호출 수로 세던 하루 AI 한도 테이블이다. 지금은 쓰지 않으므로 예전 DB에 남은 것을 지운다.
DROP TABLE IF EXISTS ai_daily_budget;

-- 순위 다시 계산 요청 표시. 행은 하나뿐이고, 처음 만들 때는 앱이 뜨면 한 번 다시 계산하도록 요청된 상태로 둔다.
INSERT OR IGNORE INTO ranking_refresh (id, requested_version, rebuilt_version) VALUES (1, 1, 0);

-- 서울 지역. parent_code는 앱이 뜰 때 RegionFacade가 코드 접두어로 채운다.
-- 서울 밖 지역은 regions.sql(scripts/regions/generate_regions.py가 만듦)이 넣는다. 서울 코드만 비공식 번호다.
INSERT OR IGNORE INTO regions(code,name,latitude,longitude) VALUES ('KR-11','서울특별시',37.5665,126.9780);
INSERT OR IGNORE INTO regions(code,name,latitude,longitude) VALUES
('KR-11-110','종로구',37.5729,126.9794),('KR-11-140','중구',37.5637,126.9975),('KR-11-170','용산구',37.5326,126.9905),('KR-11-200','성동구',37.5634,127.0369),('KR-11-230','광진구',37.5385,127.0823),('KR-11-260','동대문구',37.5744,127.0396),('KR-11-290','중랑구',37.6063,127.0925),('KR-11-320','성북구',37.5894,127.0167),('KR-11-350','강북구',37.6396,127.0257),('KR-11-380','도봉구',37.6688,127.0471),('KR-11-410','노원구',37.6542,127.0568),('KR-11-440','은평구',37.6027,126.9291),('KR-11-470','서대문구',37.5791,126.9368),('KR-11-500','마포구',37.5663,126.9014),('KR-11-530','양천구',37.5170,126.8665),('KR-11-560','강서구',37.5509,126.8495),('KR-11-590','구로구',37.4955,126.8874),('KR-11-620','금천구',37.4519,126.9020),('KR-11-650','영등포구',37.5264,126.8962),('KR-11-680','동작구',37.5124,126.9393),('KR-11-710','관악구',37.4784,126.9516),('KR-11-740','서초구',37.4837,127.0324),('KR-11-770','강남구',37.5172,127.0473),('KR-11-800','송파구',37.5145,127.1059),('KR-11-830','강동구',37.5301,127.1238);
