-- BWLOVERS sample data for PostgreSQL
-- Usage example:
--   psql -U bwlovers -d bwlovers -f docs/sample-data.sql
--
-- Prerequisite:
-- 1. Start PostgreSQL
-- 2. Run the application at least once so Hibernate creates tables
--
-- This script is designed to be re-runnable.

BEGIN;

-- Delete sample child rows first so the script can be re-run safely.
DELETE FROM evidence_sources
WHERE insurance_id IN (
    SELECT insurance_id
    FROM insurance_products
    WHERE result_id IN ('sample-rec-001', 'sample-rec-002')
);

DELETE FROM special_contracts
WHERE insurance_id IN (
    SELECT insurance_id
    FROM insurance_products
    WHERE result_id IN ('sample-rec-001', 'sample-rec-002')
);

DELETE FROM insurance_products
WHERE result_id IN ('sample-rec-001', 'sample-rec-002');

DELETE FROM simulation_contracts
WHERE simulation_id IN (
    SELECT simulation_id
    FROM simulations
    WHERE result_id = 'sample-sim-001'
);

DELETE FROM simulations
WHERE result_id = 'sample-sim-001';

DELETE FROM past_diseases
WHERE status_id IN (
    SELECT hs.status_id
    FROM health_status hs
    JOIN users u ON u.user_id = hs.user_id
    WHERE u.provider_id = 'sample-user-001'
);

DELETE FROM chronic_diseases
WHERE status_id IN (
    SELECT hs.status_id
    FROM health_status hs
    JOIN users u ON u.user_id = hs.user_id
    WHERE u.provider_id = 'sample-user-001'
);

DELETE FROM pregnancy_complications
WHERE status_id IN (
    SELECT hs.status_id
    FROM health_status hs
    JOIN users u ON u.user_id = hs.user_id
    WHERE u.provider_id = 'sample-user-001'
);

DELETE FROM health_status
WHERE user_id IN (
    SELECT user_id
    FROM users
    WHERE provider_id = 'sample-user-001'
);

DELETE FROM pregnancy_info
WHERE user_id IN (
    SELECT user_id
    FROM users
    WHERE provider_id = 'sample-user-001'
);

DELETE FROM users
WHERE provider_id = 'sample-user-001';

-- Seed basic reference data.
INSERT INTO jobs (job_name, risk_level)
VALUES
    ('사무직', 1),
    ('교사', 2),
    ('간호사', 3)
ON CONFLICT (job_name) DO NOTHING;

-- Seed one sample user.
INSERT INTO users (
    provider,
    provider_id,
    email,
    username,
    phone,
    profile_image,
    naver_access_token
)
VALUES (
    'NAVER',
    'sample-user-001',
    'sample.user@bwlovers.local',
    '샘플사용자',
    '010-1234-5678',
    'https://example.com/sample-profile.png',
    'sample-naver-access-token'
);

-- Seed pregnancy information.
INSERT INTO pregnancy_info (
    user_id,
    birth_date,
    height,
    weight_pre,
    weight_current,
    is_firstbirth,
    gestational_week,
    expected_date,
    is_multiple_pregnancy,
    miscarriage_history,
    job_id
)
SELECT
    u.user_id,
    DATE '1997-04-12',
    163,
    54,
    60,
    TRUE,
    24,
    DATE '2026-10-03',
    FALSE,
    0,
    j.job_id
FROM users u
JOIN jobs j ON j.job_name = '사무직'
WHERE u.provider_id = 'sample-user-001';

-- Seed health status and related disease data.
INSERT INTO health_status (
    user_id,
    created_at,
    updated_at
)
SELECT
    u.user_id,
    TIMESTAMP '2026-06-01 09:00:00',
    TIMESTAMP '2026-06-01 09:00:00'
FROM users u
WHERE u.provider_id = 'sample-user-001';

INSERT INTO past_diseases (
    status_id,
    past_disease_type,
    past_cured,
    past_last_treated_at
)
SELECT
    hs.status_id,
    'UTERINE_FIBROID',
    TRUE,
    DATE '2023-09-01'
FROM health_status hs
JOIN users u ON u.user_id = hs.user_id
WHERE u.provider_id = 'sample-user-001';

INSERT INTO chronic_diseases (
    status_id,
    chronic_disease_type,
    chronic_on_medication
)
SELECT
    hs.status_id,
    'ASTHMA',
    FALSE
FROM health_status hs
JOIN users u ON u.user_id = hs.user_id
WHERE u.provider_id = 'sample-user-001';

INSERT INTO pregnancy_complications (
    status_id,
    complication_type
)
SELECT
    hs.status_id,
    'NONE'
FROM health_status hs
JOIN users u ON u.user_id = hs.user_id
WHERE u.provider_id = 'sample-user-001';

-- Seed insurance recommendation result data.
INSERT INTO insurance_products (
    user_id,
    result_id,
    item_id,
    insurance_company,
    product_name,
    is_long_term,
    sum_insured,
    monthly_cost,
    insurance_recommendation_reason,
    memo,
    created_at
)
SELECT
    u.user_id,
    'sample-rec-001',
    'item-001',
    '삼성생명',
    '임신안심 종합보험',
    TRUE,
    '1억원',
    '42,000원',
    '임신 주수와 건강 상태를 고려했을 때 입원/수술 보장이 균형 있게 포함되어 있습니다.',
    '출산 전후 보장 범위를 중점적으로 확인할 예정',
    TIMESTAMP '2026-06-02 10:30:00'
FROM users u
WHERE u.provider_id = 'sample-user-001';

INSERT INTO insurance_products (
    user_id,
    result_id,
    item_id,
    insurance_company,
    product_name,
    is_long_term,
    sum_insured,
    monthly_cost,
    insurance_recommendation_reason,
    memo,
    created_at
)
SELECT
    u.user_id,
    'sample-rec-002',
    'item-002',
    '현대해상',
    '엄마든든 건강보험',
    TRUE,
    '8천만원',
    '36,500원',
    '특약 구성이 단순하고 보험료가 비교적 낮아 첫 비교용 상품으로 적합합니다.',
    '보험료 비교용 후보',
    TIMESTAMP '2026-06-02 10:35:00'
FROM users u
WHERE u.provider_id = 'sample-user-001';

INSERT INTO special_contracts (
    insurance_id,
    contract_name,
    contract_description,
    contract_recommendation_reason,
    key_features,
    page_number,
    created_at
)
SELECT
    p.insurance_id,
    '임신중독증 진단 특약',
    '임신중독증 진단 시 보험금을 지급하는 특약입니다.',
    '임신 합병증 보장을 강화하려는 목적에 적합합니다.',
    '진단금 지급, 약관상 보장 조건 확인 필요',
    12,
    TIMESTAMP '2026-06-02 10:31:00'
FROM insurance_products p
WHERE p.result_id = 'sample-rec-001'
  AND p.item_id = 'item-001';

INSERT INTO special_contracts (
    insurance_id,
    contract_name,
    contract_description,
    contract_recommendation_reason,
    key_features,
    page_number,
    created_at
)
SELECT
    p.insurance_id,
    '조산아 입원 특약',
    '조산아 치료 및 입원 관련 비용을 보장하는 특약입니다.',
    '출산 직후 리스크 대응을 강화할 수 있습니다.',
    '신생아 집중치료실 관련 확인 필요',
    18,
    TIMESTAMP '2026-06-02 10:32:00'
FROM insurance_products p
WHERE p.result_id = 'sample-rec-001'
  AND p.item_id = 'item-001';

INSERT INTO evidence_sources (
    insurance_id,
    page_number,
    text_snippet,
    created_at
)
SELECT
    p.insurance_id,
    12,
    '피보험자가 약관에서 정한 임신중독증으로 진단받은 경우 보험금을 지급합니다.',
    TIMESTAMP '2026-06-02 10:33:00'
FROM insurance_products p
WHERE p.result_id = 'sample-rec-001'
  AND p.item_id = 'item-001';

INSERT INTO evidence_sources (
    insurance_id,
    page_number,
    text_snippet,
    created_at
)
SELECT
    p.insurance_id,
    18,
    '조산아로 입원 치료를 받은 경우 입원 일수에 따라 보험금을 지급합니다.',
    TIMESTAMP '2026-06-02 10:34:00'
FROM insurance_products p
WHERE p.result_id = 'sample-rec-002'
  AND p.item_id = 'item-002';

-- Seed simulation result data.
INSERT INTO simulations (
    result_id,
    user_id,
    insurance_company,
    product_name,
    question,
    result,
    created_at
)
SELECT
    'sample-sim-001',
    u.user_id,
    '삼성생명',
    '임신안심 종합보험',
    '임신 24주, 천식 병력이 있을 때 이 특약 구성이 적절한가요?',
    '현재 건강 상태와 임신 주수를 고려하면 입원/합병증 특약 위주의 구성이 우선순위가 높습니다.',
    TIMESTAMP '2026-06-03 15:00:00'
FROM users u
WHERE u.provider_id = 'sample-user-001';

INSERT INTO simulation_contracts (
    simulation_id,
    contract_name,
    page_number,
    created_at
)
SELECT
    s.simulation_id,
    '임신중독증 진단 특약',
    12,
    TIMESTAMP '2026-06-03 15:01:00'
FROM simulations s
WHERE s.result_id = 'sample-sim-001';

INSERT INTO simulation_contracts (
    simulation_id,
    contract_name,
    page_number,
    created_at
)
SELECT
    s.simulation_id,
    '조산아 입원 특약',
    18,
    TIMESTAMP '2026-06-03 15:02:00'
FROM simulations s
WHERE s.result_id = 'sample-sim-001';

COMMIT;
