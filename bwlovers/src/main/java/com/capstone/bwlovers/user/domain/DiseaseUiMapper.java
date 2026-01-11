package com.capstone.bwlovers.user.domain;

import com.capstone.bwlovers.user.domain.healthType.PastDiseaseType;

import java.util.Map;

public class DiseaseUiMapper {

    private static final Map<PastDiseaseType, UiSubCategory> PAST_MAP = Map.of(
            PastDiseaseType.UTERINE_FIBROID, UiSubCategory.GYNECOLOGY,
            PastDiseaseType.ENDOMETRIOSIS, UiSubCategory.GYNECOLOGY,
            PastDiseaseType.OVARIAN_CYST, UiSubCategory.GYNECOLOGY,

            PastDiseaseType.HYPERTENSION, UiSubCategory.INTERNAL_MEDICINE,
            PastDiseaseType.DIABETES, UiSubCategory.INTERNAL_MEDICINE,
            PastDiseaseType.THYROID_DISEASE, UiSubCategory.INTERNAL_MEDICINE,

            PastDiseaseType.ABDOMINAL_SURGERY, UiSubCategory.SURGERY_HISTORY,
            PastDiseaseType.OTHER_SURGERY, UiSubCategory.SURGERY_HISTORY,

            PastDiseaseType.DEPRESSION, UiSubCategory.MENTAL_HEALTH,
            PastDiseaseType.ANXIETY_DISORDER, UiSubCategory.MENTAL_HEALTH
    );

    public static UiSubCategory getSubCategory(PastDiseaseType type) {
        return PAST_MAP.getOrDefault(type, UiSubCategory.ETC);
    }
}
